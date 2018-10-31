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
import java.math.BigDecimal
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters

class ConcreteServiceExecutor(private val service: Any) : ServiceExecutor {
  companion object {
    private val log = loggerFor<ConcreteServiceExecutor>()
  }

  override fun invoke(request: JsonRPCRequest): Observable<Any> {
    return Observable.create<Any> { subscriber ->
      try {
        orderByComplexity(service::class.functions
          .filter(request::matchesName)
          .filter(this::isPublic)
          .filter { it.parameters.size == request.paramCount() + 1 }
        )
          .asSequence() // lazy sequence
          .filterMethodsAndConvertedParameters(request)
          .map { (method, params) -> method.call(service, *params) }
          .firstOrNull()
          ?.also { result -> handleResult(result, request, subscriber) }
          ?: throwMethodDoesNotExist(request)
      } catch (err: InvocationTargetException) {
        log.trace("failed to invoke target for $request", err)
        subscriber.onError(err.targetException)
      } catch (err: Throwable) {
        log.debug("failed to invoke $request", err)
        subscriber.onError(err)
      }
    }
  }

  private fun throwMethodDoesNotExist(request: JsonRPCRequest) {
    throw MethodDoesNotExist("failed to find a method that matches ${request.method}(${request.paramsAsString()})")
  }

  private fun Sequence<KFunction<*>>.filterMethodsAndConvertedParameters(request: JsonRPCRequest): Sequence<Pair<KFunction<*>, Array<Any?>>> {
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
    when (result) {
      is Future<*> -> handleFuture(result as Future<Any>, request, subscriber)
      is Observable<*> -> handleObservable(result as Observable<Any>, request, subscriber)
      else -> respond(result, subscriber)
    }
  }

  private fun handleObservable(result: Observable<Any>, request: JsonRPCRequest, subscriber: Subscriber<Any>) {
    result
      .onErrorResumeNext { err -> Observable.error(err.createJsonException(request)) }
      .subscribe(subscriber)
  }

  private fun handleFuture(future: Future<Any>, request: JsonRPCRequest, callback: Subscriber<Any>) {
    future.setHandler(JsonRPCMounter.FutureHandler {
      handleAsyncResult(it, request, callback)
    })
  }

  private fun handleAsyncResult(response: AsyncResult<*>, request: JsonRPCRequest, subscriber: Subscriber<Any>) {
    when (response.succeeded()) {
      true -> respond(response.result(), subscriber)
      else -> respond(response.cause().createJsonException(request), subscriber)
    }
  }

  private fun respond(result: Any?, subscriber: Subscriber<Any>) {
    subscriber.onNext(result)
    subscriber.onCompleted()
  }

  private fun respond(err: Throwable, subscriber: Subscriber<Any>) {
    subscriber.onError(err)
  }

  private fun orderByComplexity(methods: List<KFunction<*>>): List<KFunction<*>> {
    return methods.sortedWith(compareByDescending(this::sumTypes))
  }

  private fun sumTypes(method: KFunction<*>) = method.valueParameters.asSequence().map(this::typeValue).sum()

  private fun typeValue(parameter: KParameter): Int = when (parameter.type.classifier) {
    BigDecimal::class -> -1
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