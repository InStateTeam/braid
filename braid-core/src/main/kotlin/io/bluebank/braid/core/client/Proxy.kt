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

package io.bluebank.braid.core.client

import io.bluebank.braid.core.annotation.MethodDescription
import io.bluebank.braid.core.jsonrpc.JsonRPCResponse
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.RequestOptions
import io.vertx.core.http.WebSocket
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import rx.Observable
import rx.Subscriber
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

data class ServiceEndpoint(val host : String = "localhost",
                           val ssl : Boolean = false,
                           val path : String = "/api/",
                           val port : Int = 8080,
                           val credentials: JsonObject? = null)

private class InvocationClosure(val subscriber: Subscriber<*>, returnType: Class<*>)

class BraidInvocationHandler(endpoint: ServiceEndpoint) : InvocationHandler {
  private val vertx = Vertx.vertx()
  private val client : HttpClient
  private lateinit var socket: WebSocket
  private val nextId = AtomicLong()
  private val activeRequests = mutableMapOf<Long, InvocationClosure>()

  init {
    client = vertx.createHttpClient()
    val ro = RequestOptions()
        .setHost(endpoint.host)
        .setPort(endpoint.port)
        .setSsl(endpoint.ssl)
        .setURI(endpoint.path)
    client.websocket(ro) { ws ->
      socket = ws
      socket.handler(this::handler)
    }
  }

  protected fun finalize() {
    client.close()
    vertx.close()
  }

  override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any {
    return when {
      (method.returnType == Observable::class.java) -> {
        return invokeForObservable(proxy, method, args)
      }
      (method.returnType == Future::class.java) -> {
        return invokeForFuture(proxy, method, args)
      }
      (method.returnType.simpleName == "void") -> {
        return invokeForVoid(proxy, method, args)
      }
      else -> {
        throw RuntimeException("return type unhandled: " + method.returnType.name)
      }
    }
  }

  private fun invokeForVoid(proxy: Any, method: Method, args: Array<out Any>): Unit {
    invokeForObservable(proxy, method, args).subscribe()
  }

  private fun invokeForFuture(proxy: Any, method: Method, args: Array<out Any>): Future<*> {
    val result = Future.future<Any>()
    invokeForObservable(proxy, method, args)
        .subscribe(result::complete, result::fail)
    return result
  }

  private fun invokeForObservable(proxy: Any, method: Method, args: Array<out Any>): Observable<*> {
    val md = method.getAnnotation<MethodDescription>(MethodDescription::class.java)
    return Observable.create<Any> {

    }
  }

  private fun handler(buffer: Buffer) {
    Json.decodeValue(buffer, JsonRPCResponse::class.java)
  }
}

inline fun <reified T : Any> KClass<T>.braidProxy(endpoint: ServiceEndpoint) = braidProxy(T::class.java, endpoint)

fun <T: Any> braidProxy(clazz: Class<T>, endpoint: ServiceEndpoint) : T {
  val loader = clazz.classLoader
  val interfaces = arrayOf(clazz)
  val invocationHandler = BraidInvocationHandler(endpoint)
  @Suppress("UNCHECKED_CAST")
  return Proxy.newProxyInstance(loader, interfaces, invocationHandler) as T
}