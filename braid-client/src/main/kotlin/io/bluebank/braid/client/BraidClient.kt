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
package io.bluebank.braid.client

import io.bluebank.braid.core.async.getOrThrow
import io.bluebank.braid.core.async.toFuture
import io.bluebank.braid.core.json.BraidJacksonInit
import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.reflection.actualType
import io.bluebank.braid.core.reflection.isStreaming
import io.bluebank.braid.core.reflection.underlyingGenericType
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocket
import io.vertx.core.http.WebSocketFrame
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import rx.Observable
import rx.subjects.PublishSubject
import java.io.Closeable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

open class BraidClient(private val config: BraidClientConfig, val vertx: Vertx, exceptionHandler: (Throwable) -> Unit = this::exceptionHandler, closeHandler: (() -> Unit) = this::closeHandler) : Closeable, InvocationHandler {
  private val nextId = AtomicLong(1)
  private val invocations = ConcurrentHashMap<Long, ProxyInvocation>()
  private var sock: WebSocket? = null

  private val client = vertx.createHttpClient(HttpClientOptions()
      .setDefaultHost(config.serviceURI.host)
      .setDefaultPort(config.serviceURI.port)
      .setSsl(config.tls)
      .setVerifyHost(config.verifyHost)
      .setTrustAll(config.trustAll))

  companion object {
    private val log: Logger = loggerFor<BraidClient>()

    init {
      BraidJacksonInit.init()
    }

    fun createClient(config: BraidClientConfig, vertx: Vertx = Vertx.vertx()): BraidClient {
      return BraidClient(config, vertx)
    }

    private fun closeHandler() {
      log.info("closing...")
    }

    private fun exceptionHandler(error: Throwable) {
      log.error("exception from socket", error)
      // TODO: handle retries?
      // TODO: handle error!
    }
  }

  init {
    val protocol = if (config.tls) "https" else "http"
    val url = URL(protocol, config.serviceURI.host, config.serviceURI.port, "${config.serviceURI.path}/websocket")
    val result = future<Boolean>()
    client.websocket(url.toString(), { socket ->
      try {
        sock = socket
        socket.handler(this::handler)
        socket.exceptionHandler(exceptionHandler)
        socket.closeHandler(closeHandler)
        result.complete(true)
      } catch (err: Throwable) {
        log.error("failed to connect socket to the client stack", err)
        sock = null
        result.fail(err)
      }
    }, { error ->
      log.error("failed to bind to websocket", error)
      try {
        sock = null
        result.fail(error)
      } catch (err: Throwable) {
        log.error("failed to report error on result future", err)
      }
    })
    result.getOrThrow()
  }

  fun activeRequestsCount(): Int {
    return invocations.size
  }

  @Suppress("UNCHECKED_CAST")
  fun <ServiceType : Any> bind(clazz: Class<ServiceType>): ServiceType {
    return Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz), this) as ServiceType
  }

  override fun close() {
    try {
      client.close()
    } finally {
      vertx.close()
    }
  }

  override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
    val socket = sock
    if (socket != null) {
      return jsonRPC(socket, method.name, method.genericReturnType, *(args ?: arrayOfNulls<Any>(0))).awaitResult()
    }
    throw IllegalStateException("no socket for proxy")
  }

  private fun handler(buffer: Buffer) {
    val jo = JsonObject(buffer)
    val responseId = jo.getLong("id")
    try {
      when {
        responseId == null -> log.error("received response without id {}", buffer.toString())
        !invocations.containsKey(responseId) -> log.error("no subscriber found for response id {}", responseId)
        else -> handleInvocationWithResponse(responseId, jo)
      }
    } catch (err: Throwable) {
      log.error("failed to handle response message", err)
    }
  }

  private fun handleInvocationWithResponse(responseId: Long, jo: JsonObject) {
    try {
      invocations[responseId]!!.handle(jo)
    } catch (err: Throwable) {
      invocations[responseId]!!.onError(err)
    }
  }

  private fun jsonRPC(socket: WebSocket, method: String, returnType: Type, vararg params: Any?): ProxyInvocation {
    val id = nextId.getAndIncrement()
    val proxyInvocation = ProxyInvocation(returnType)
    invocations[id] = proxyInvocation
    try {
      val request = JsonRPCRequest(id = id, method = method, params = params.toList(), streamed = returnType.isStreaming())
      vertx.runOnContext {
        try {
          socket.writeFrame(WebSocketFrame.textFrame(Json.encode(request), true))
        } catch (e: IllegalStateException) {
          onRequestError(id, e)
        }
      }
    } catch (err: Throwable) {
      onRequestError(id, err)
    }
    return proxyInvocation
  }

  private fun onRequestError(id: Long, err: Throwable) {
    val proxyInvocation = invocations[id]
    if (proxyInvocation != null) {
      proxyInvocation.onError(err)
      invocations.remove(id)
    } else {
      log.warn("could not find invocation object $id to report exception", err)
    }
  }

  private inner class ProxyInvocation(private val returnType: Type) {

    private val payloadType = Json.mapper.typeFactory.constructType(returnType.underlyingGenericType())
    private val resultStream = PublishSubject.create<Any>()

    fun awaitResult(): Any {
      return when (returnType.actualType()) {
        Future::class.java -> {
          resultStream.toSingle().toFuture()
        }
        Observable::class.java -> {
          resultStream
        }
        else -> {
          resultStream.toBlocking().first()
        }
      }
    }

    fun handle(jo: JsonObject) {
      if (!jo.containsKey("id")) {
        log.error("response object does not contain id key: $jo")
        return
      }

      val responseId = jo.getLong("id")
      when {
        jo.containsKey("result") -> {
          val raw = jo.getValue("result")
          val result = Json.mapper.convertValue<Any>(raw, payloadType)

          // TODO: this is moderately horrific - otherwise it's hard to make assertions about the number of handlers
          if (returnType.isStreaming()) {
            resultStream.onNext(result)
          } else {
            invocations.remove(responseId)
            resultStream.onNext(result)
            resultStream.onCompleted()
          }
        }
        jo.containsKey("error") -> {
          val error= jo.getJsonObject("error")
          invocations.remove(responseId)
          onError(RuntimeException(error.getString("message")))
        }
        jo.containsKey("completed") -> {
          if (!returnType.isStreaming()) {
            log.error("Not expecting completed messages for anything other than Observables")
          }

          invocations.remove(responseId)
          resultStream.onCompleted()
        }
      }
    }

    fun onError(err: Throwable) {
      resultStream.onError(err)
    }
  }
}

