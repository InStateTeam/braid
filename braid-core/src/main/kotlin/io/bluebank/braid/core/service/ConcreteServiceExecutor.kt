/**
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bluebank.braid.core.service

import io.bluebank.braid.core.jsonrpc.JsonRPCMounter
import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.jsonrpc.createJsonException
import io.bluebank.braid.core.jsonschema.toDescriptor
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import rx.Observable
import rx.Subscriber
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaType

class ConcreteServiceExecutor(private val service: Any) : ServiceExecutor {
  companion object {
    private val log = loggerFor<ConcreteServiceExecutor>()
  }

  override fun invoke(request: JsonRPCRequest): Observable<Any> {
    return Observable.create<Any> { subscriber ->
      try {
        log.trace("{} - binding to method for {}", request.id, request)
        candidateMethods(request)
          .asSequence() // lazy sequence
          .convertParametersAndFilter(request)
          .map { (method, params) ->
            if (log.isTraceEnabled) {
              log.trace("${request.id} - invoking ${method.asSimpleString()} with ${params.joinToString(",") { it.toString() }}")
            }
            method.call(service, *params).also {
              if (log.isTraceEnabled) {
                log.trace("${request.id} - successfully invoked ${method.asSimpleString()} with ${params.joinToString(",") { it.toString() }}")
              }
            }
          }
          .firstOrNull()
          ?.also { result -> handleResult(result, request, subscriber) }
          ?: throwMethodDoesNotExist(request)
      } catch (err: InvocationTargetException) {
        log.error("${request.id} - failed to invoke target for $request", err)
        subscriber.onError(err.targetException)
      } catch (err: Throwable) {
        log.error("${request.id} - failed to invoke $request", err)
        subscriber.onError(err)
      }
    }
  }

  private fun KFunction<*>.asSimpleString() : String {
    val params = this.parameters.drop(1).joinToString(",") { "${it.name}: ${it.type.javaType.typeName}"}
    return "$name($params)"
  }

  private fun candidateMethods(request: JsonRPCRequest) : List<KFunction<*>> {
    return service::class.functions
      .filter(request::matchesName)
      .filter(this::isPublic)
      .filter { it.parameters.size == request.paramCount() + 1 }
      .map { it to request.computeScore(it) }
      .filter { (_, score) -> score > 0 }
      .sortedByDescending { (_, score) -> score }
      .also {
        if (log.isTraceEnabled) {
          log.trace("{} scores for candidate methods for {}:", request.id, request)
          it.forEach {
            println("${it.second}: ${it.first.asSimpleString()}")
          }
        }
      }
      .map { (fn, _) -> fn }
  }


  private fun throwMethodDoesNotExist(request: JsonRPCRequest) {
    throw MethodDoesNotExist("failed to find a method that matches ${request.method}(${request.paramsAsString()})")
  }

  private fun Sequence<KFunction<*>>.convertParametersAndFilter(request: JsonRPCRequest): Sequence<Pair<KFunction<*>, Array<Any?>>> {
    // attempt to convert the parameters
    return map { method ->
      method to try {
        request.mapParams(method)
      } catch (err: Throwable) {
        null
      }
    }
      // filter out parameters that didn't match
      .filter { (_, params) -> params != null }
      .map { (method, params) -> method to params!! }
  }

  @Suppress("UNCHECKED_CAST")
  private fun handleResult(result: Any?, request: JsonRPCRequest, subscriber: Subscriber<Any>) {
    log.trace("{} - handling result {}", request.id, result)
    when (result) {
      is Future<*> -> handleFuture(result as Future<Any>, request, subscriber)
      is Observable<*> -> handleObservable(result as Observable<Any>, request, subscriber)
      else -> respond(request.id, result, subscriber)
    }
  }

  private fun handleObservable(result: Observable<Any>, request: JsonRPCRequest, subscriber: Subscriber<Any>) {
    log.trace("{} - handling observable result", request.id)
    result
      .onErrorResumeNext { err -> Observable.error(err.createJsonException(request)) }
      .let { // insert logger if trace is enabled
        if (log.isTraceEnabled) {
          it
            .doOnNext {
            log.trace("{} - sending item {}", request.id, it)
          }
            .doOnError {
              log.trace("{} - sending error {}", request.id, it)
            }
            .doOnCompleted {
              log.trace("{} - completing stream", request.id)
            }
        } else {
          it
        }
      }
      .subscribe(subscriber)
  }

  private fun handleFuture(future: Future<Any>, request: JsonRPCRequest, callback: Subscriber<Any>) {
    log.trace("{} - handling future result", request.id)
    future.setHandler(JsonRPCMounter.FutureHandler {
      handleAsyncResult(it, request, callback)
    })
  }

  private fun handleAsyncResult(response: AsyncResult<*>, request: JsonRPCRequest, subscriber: Subscriber<Any>) {
    log.trace("{} - handling async result of invocation", request.id)
    when (response.succeeded()) {
      true -> respond(request.id, response.result(), subscriber)
      else -> respond(request.id, response.cause().createJsonException(request), subscriber)
    }
  }

  private fun respond(id: Long, result: Any?, subscriber: Subscriber<Any>) {
    log.trace("{} - sending result and completing {}", id, result)
    subscriber.onNext(result)
    subscriber.onCompleted()
  }

  private fun respond(id: Long, err: Throwable, subscriber: Subscriber<Any>) {
    log.trace("{} - sending error {}", id, err)
    subscriber.onError(err)
  }

  private fun orderByComplexity(methods: List<KFunction<*>>): List<KFunction<*>> {
    return methods.sortedWith(compareByDescending(this::sumTypes))
  }

  private fun sumTypes(method: KFunction<*>) = method.valueParameters.asSequence().map(this::typeValue).sum()

  private fun typeValue(parameter: KParameter): Int = when (parameter.type.classifier) {
    String::class -> 0
    Int::class, Float::class -> 1
    Double::class, Long::class -> 2
    List::class -> 4
    Map::class -> 5
    else -> {
      when {
        parameter.type.javaClass.isArray -> 3
        else -> 6
      }
    }
  }

  private fun isPublic(method: KFunction<*>) = method.visibility == KVisibility.PUBLIC

  override fun getStubs(): List<MethodDescriptor> {
    return service.javaClass.declaredMethods
      .filter { Modifier.isPublic(it.modifiers) }
      .filter { !it.name.contains("$") }
      .map { it.toDescriptor() }
  }
}