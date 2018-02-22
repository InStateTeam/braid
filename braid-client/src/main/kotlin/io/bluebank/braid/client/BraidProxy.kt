package io.bluebank.braid.client

import io.bluebank.braid.core.async.getOrThrow
import io.bluebank.braid.core.async.toFuture
import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.reflection.underlyingGenericType
import io.bluebank.braid.core.reflection.actualType
import io.bluebank.braid.core.reflection.isStreaming
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
import java.lang.reflect.*
import java.net.URL
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong


class BraidProxyClient(private val config: BraidClientConfig, val vertx: Vertx) : Closeable, InvocationHandler  {
  private val nextId = AtomicLong(1)
  private val invocations = mutableMapOf<Long, ProxyInvocation>()
  private val sockets = mutableMapOf<Class<*>, WebSocket>()

  private val client = vertx.createHttpClient(HttpClientOptions()
      .setDefaultHost(config.serviceURI.host)
      .setDefaultPort(config.serviceURI.port)
      .setSsl(config.tls)
      .setVerifyHost(config.verifyHost)
      .setTrustAll(config.trustAll))

  companion object {
    private val log: Logger = loggerFor<BraidProxyClient>()

    fun <T : Any> createProxyClient(config: BraidClientConfig, vertx: Vertx = Vertx.vertx()): BraidProxyClient {
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
      val proxy = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz), this) as ServiceType
      sockets.put(clazz, socket)
      socket.handler(this::handler)
      socket.exceptionHandler(exceptionHandler)
      socket.closeHandler(closeHandler)
      result.complete(proxy)
    }, { error -> result.fail(error) })
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
    try {
      val jo = JsonObject(buffer)
      val responseId = jo.getLong("id")
      when {
        responseId == null -> log.error("received response without id {}", buffer.toString())
        !invocations.containsKey(responseId) -> log.error("no subscriber found for response id {}", responseId)
        else -> invocations[responseId]!!.handle(jo)
      }
    } catch (err: Throwable) {
      log.error("failed to handle response message", err)
    }
  }

  private fun closeHandler() {
      log.info("closing...")
  }

  private fun exceptionHandler(error: Throwable) {
    log.error("exception from socket", error)
    // TODO: handle retries?
    // TODO: handle error!
  }

  private fun jsonRPC(socket: WebSocket, method: String, returnType: Type, vararg params: Any?): ProxyInvocation {
    val id = nextId.incrementAndGet()
    val proxyInvocation = ProxyInvocation(returnType)
    invocations.put(id, proxyInvocation)
    try {
      val request = JsonRPCRequest(id = id, method = method, params = params.toList(), streamed = returnType.isStreaming())
      socket.writeFrame(WebSocketFrame.textFrame(Json.encode(request), true))
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

fun WebSocket.closeHandler(fn: () -> Unit) {
    this.closeHandler {
        fn()
    }
}