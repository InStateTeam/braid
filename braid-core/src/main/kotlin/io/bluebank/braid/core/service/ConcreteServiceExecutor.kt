/*
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

import io.bluebank.braid.core.jsonrpc.JsonRPCErrorResponse
import io.bluebank.braid.core.jsonrpc.JsonRPCMounter
import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.jsonrpc.createJsonException
import io.bluebank.braid.core.jsonschema.toDescriptor
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import rx.Observable
import rx.Subscriber
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class ConcreteServiceExecutor(private val service: Any) : ServiceExecutor {

  override fun invoke(request: JsonRPCRequest): Observable<Any> {
    // use unsafe constructor until we switch to rxjava2 and vertx 3.5.0 - currently too new to be confident of stability
    return Observable.create<Any> { subscriber ->
      try {
        val method = findMethod(request)
        val castedParameters = request.mapParams(method)
        val result = method.invoke(service, *castedParameters)
        handleResult(result, request, subscriber)
      } catch (err: InvocationTargetException) {
        subscriber.onError(err.targetException)
      } catch (err: Throwable) {
        subscriber.onError(err)
      }
    }
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

  private fun findMethod(request: JsonRPCRequest): Method {
    try {
      return service.javaClass.methods.single { request.matchesMethod(it) }
    } catch (err: IllegalArgumentException) {
      JsonRPCErrorResponse.throwMethodNotFound(request.id, "method ${request.method} has multiple implementations with the same number of parameters")
    } catch (err: NoSuchElementException) {
      throw MethodDoesNotExist(request.method)
    }
  }

  override fun getStubs(): List<MethodDescriptor> {
    return service.javaClass.declaredMethods
        .filter { Modifier.isPublic(it.modifiers) }
        .filter { !it.name.contains("$")}
        .map { it.toDescriptor() }
  }
}