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

/**
 * Deprecated. Used [BraidClient]
 */
@Deprecated("please use BraidClient instead - this will be removed in 4.0.0 - see issue #75", replaceWith = ReplaceWith("BraidClient"))
open class BraidProxyClient(private val config: BraidClientConfig, val vertx: Vertx) : Closeable, InvocationHandler {
  private val nextId = AtomicLong(1)
  private val invocations = ConcurrentHashMap<Long, ProxyInvocation>()
  private val sockets = mutableMapOf<Class<*>, WebSocket>()

  private val client = vertx.createHttpClient(HttpClientOptions()
    .setDefaultHost(config.serviceURI.host)
    .setDefaultPort(config.serviceURI.port)
    .setSsl(config.tls)
    .setVerifyHost(config.verifyHost)
    .setTrustAll(config.trustAll))

  companion object {
    private val log: Logger = loggerFor<BraidProxyClient>()

    init {
      BraidJacksonInit.init()
    }

    fun createProxyClient(config: BraidClientConfig, vertx: Vertx = Vertx.vertx()): BraidProxyClient {
      return BraidProxyClient(config, vertx)
    }
  }

  fun activeRequestsCount(): Int {
    return invocations.size
  }

  fun <ServiceType : Any> bind(clazz: Class<ServiceType>, exceptionHandler: (Throwable) -> Unit = this::exceptionHandler, closeHandler: (() -> Unit) = this::closeHandler): ServiceType {
    return bindAsync(clazz, exceptionHandler, closeHandler).getOrThrow()
  }

  // TODO: fix the obvious lunacy of only having one handler per socket...
  @Suppress("UNCHECKED_CAST")
  fun <ServiceType : Any> bindAsync(clazz: Class<ServiceType>, exceptionHandler: (Throwable) -> Unit = this::exceptionHandler, closeHandler: (() -> Unit) = this::closeHandler): Future<ServiceType> {
    val result = future<ServiceType>()
    val url = URL("https", config.serviceURI.host, config.serviceURI.port, "${config.serviceURI.path}/websocket")

    client.websocket(url.toString(), { socket ->
      try {
        val proxy = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz), this) as ServiceType
        sockets[clazz] = socket
        socket.handler(this::handler)
        socket.exceptionHandler(exceptionHandler)
        socket.closeHandler(closeHandler)
        result.complete(proxy)
      } catch (err: Throwable) {
        log.error("failed to connect socket to the client stack", err)
        sockets.remove(clazz)
        socket.handler { }
        socket.exceptionHandler { }
        socket.closeHandler { }
      }
    }, { error ->
      log.error("failed to bind to websocket", error)
      try {
        result.fail(error)
      } catch (err: Throwable) {
        log.error("failed to report error on result future", err)
      }
    })
    return result
  }

  override fun close() {
    try {
      client.close()
    } finally {
      vertx.close()
    }
  }

  override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any {
    val socket = sockets[method.declaringClass]
    if (socket != null) {
      return jsonRPC(socket, method.name, method.genericReturnType, *(args ?: arrayOfNulls<Any>(0))).awaitResult()
    }
    throw IllegalStateException("no socket for proxy")
  }

  private fun handler(buffer: Buffer) {
    val jo = JsonObject(buffer)
    if (!jo.containsKey("id")) {
      log.warn("received message without 'id' field from ${config.serviceURI}")
      return
    }
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
    val proxy = invocations[responseId]
    if (proxy == null) {
      log.error("{} - failed to find invocation proxy", responseId)
      return
    }

    try {
      proxy.handle(jo)
    } catch (err: Throwable) {
      log.error("{} - failed to handle message. sending to error handler {}", responseId, jo.encode())
      try {
        proxy.onError(err)
      } catch (err: Throwable) {
        log.error("$responseId - failed to send handler exception to subject", err)
      }
      invocations.remove(responseId)
    }
  }


  private fun closeHandler() {
    log.info("closing proxy to {}", config.serviceURI)
  }

  private fun exceptionHandler(error: Throwable) {
    log.error("exception from socket", error)
    // TODO: handle retries?
    // TODO: handle error!
  }

  private fun jsonRPC(socket: WebSocket, method: String, returnType: Type, vararg params: Any?): ProxyInvocation {
    log.trace("invoking", method)
    val id = nextId.getAndIncrement()
    val proxyInvocation = ProxyInvocation(id, returnType)
    invocations[id] = proxyInvocation
    try {
      val request = JsonRPCRequest(id = id, method = method, params = params.toList(), streamed = returnType.isStreaming())
      vertx.runOnContext {
        try {
          log.trace("{} - sending request {}", request.id, request)
          val payload = Json.encode(request)
          log.trace("{} - writing to websocket {}", request.id, payload)
          socket.writeFrame(WebSocketFrame.textFrame(payload, true))
          log.trace("{} - wrote to websocket request {}", request.id, payload)
        } catch (err: Throwable) {
          log.error("${request.id} - failed to write request to websocket", err)
        }
      }
    } catch (err: Throwable) {
      log.trace("$id - failed to invoke jsonRPC", err)
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
      log.warn("$id - could not find invocation object to report exception", err)
    }
  }

  private inner class ProxyInvocation(private val invocationId: Long, private val returnType: Type) {

    private val payloadType = Json.mapper.typeFactory.constructType(returnType.underlyingGenericType())

    private val resultBuffer = PublishSubject.create<Any>()

    fun awaitResult(): Any {
      return when (returnType.actualType()) {
        Future::class.java -> {
          resultBuffer
            .doOnNext { log.trace("received {}", it) }
            .doOnError { log.trace("received error", it)}
            .doOnCompleted { log.trace("received completion")}
            .toSingle().toFuture()
        }
        Observable::class.java -> {
          resultBuffer
        }
        else -> {
          resultBuffer.toBlocking().first()
        }
      }
    }

    fun handle(jo: JsonObject) {
      if (log.isTraceEnabled) {
        log.trace("handling received message {}", jo.encode())
      }
      when {
        jo.containsKey("result") -> {
          val raw = jo.getValue("result")
          val result = Json.mapper.convertValue<Any>(raw, payloadType)

          // TODO: this is moderately horrific - otherwise it's hard to make assertions about the number of handlers
          if (returnType.isStreaming()) {
            log.trace("{} - pushing message to streaming subject: {}", invocationId, result)
            resultBuffer.onNext(result)
          } else {
            log.trace("{} - pushing message to non-streaming subject and completing the subject: {}", invocationId, result)
            invocations.remove(invocationId)
            resultBuffer.onNext(result)
            resultBuffer.onCompleted()
          }
        }
        jo.containsKey("error") -> {
          log.trace("{} - message is an error response. terminating the subject", invocationId)
          val error = jo.getJsonObject("error")
          invocations.remove(invocationId)
          onError(RuntimeException(error.getString("message")))
        }
        jo.containsKey("completed") -> {
          log.trace("{} - message is a completion response", invocationId)
          if (!returnType.isStreaming()) {
            log.error("Not expecting completed messages for anything other than Observables")
          }

          invocations.remove(invocationId)
          resultBuffer.onCompleted()
        }
      }
    }

    fun onError(err: Throwable) {
      resultBuffer.onError(err)
    }
  }
}

fun WebSocket.closeHandler(fn: () -> Unit) {
  this.closeHandler {
    fn()
  }
}