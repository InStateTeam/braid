package io.bluebank.braid.client

import io.bluebank.braid.core.async.toFuture
import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.jsonrpc.JsonRPCResponse
import io.bluebank.braid.core.jsonrpc.JsonRPCResultResponse
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.reflection.actualReturnType
import io.bluebank.braid.core.reflection.serviceName
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocket
import io.vertx.core.http.WebSocketFrame
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import org.slf4j.Logger
import rx.Observable
import rx.subjects.PublishSubject
import java.io.Closeable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.lang.reflect.Type
import java.net.URL
import java.util.concurrent.atomic.AtomicLong


class BraidProxy<ServiceType : Any>(private val clazz: Class<ServiceType>, private val config: BraidClientConfig) : Closeable {
  private val vertx: Vertx = Vertx.vertx()

  private val client = vertx.createHttpClient(HttpClientOptions()
      .setDefaultHost(config.serviceURI.host)
      .setDefaultPort(config.serviceURI.port)
      .setSsl(config.tls)
      .setVerifyHost(config.verifyHost)
      .setTrustAll(config.trustAll))


  companion object {
    inline fun <reified T : Any> createProxy(config: BraidClientConfig): BraidProxy<T> {
      return createProxy(T::class.java, config)
    }

    fun <T : Any> createProxy(clazz: Class<T>, config: BraidClientConfig): BraidProxy<T> {
      return BraidProxy(clazz, config)
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun bind(): Future<ServiceType> {
    val result = future<ServiceType>()
    // TODO find nicer way to deal with URL not understanding the ws protocol
//    val url = URL("https", config.serviceURI.host, config.serviceURI.port, "${config.serviceURI.path}/${clazz.serviceName()}/websocket")
    val url = "https://${config.serviceURI.host}:${config.serviceURI.port}${config.serviceURI.path}/websocket"

    client.websocket(url, { socket ->
      val proxy = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz), ProxyInvocationHandler(clazz, socket, config)) as ServiceType
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
}

private class ProxyInvocationHandler<T : Any>(private val clazz: Class<T>, private val socket: WebSocket, private val config: BraidClientConfig) : InvocationHandler {
  private val nextId = AtomicLong(1)
  // TODO: need a timeout in here so we can hoover up old invocations and not run out of memory...
  private val invocations = mutableMapOf<Long, ProxyInvocation>()

  companion object {
    private val log: Logger = loggerFor<ProxyInvocationHandler<*>>()
  }

  init {
    socket.handler(this::handler)
    socket.exceptionHandler(this::exceptionHandler)
  }

  override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any {
    return jsonRPC(method.name, method.genericReturnType, *args).awaitResult()
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
      // TODO: do we need to handle the error here? and throw it?
    }
  }

  private fun exceptionHandler(error: Throwable) {
    log.error("exception from socket", error)
    // TODO: handle retries?
    // TODO: handle error!
  }

  private fun jsonRPC(method: String, returnType: Type, vararg params: Any?): ProxyInvocation {
    val id = nextId.incrementAndGet()
    val proxyInvocation = ProxyInvocation(returnType)
    invocations.put(id, proxyInvocation)
    try {
      val request = JsonRPCRequest(id = id, method = method, params = params.toList())
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
}

private class ProxyInvocation(private val returnType: Type) {
  private val payloadType = Json.mapper.typeFactory.constructType(returnType.actualReturnType())

  private val resultStream = PublishSubject.create<Any>()

  fun awaitResult(): Any {
    return when (returnType) {
      is Future<*> -> {
        resultStream.toSingle().toFuture()
      }
      is Observable<*> -> {
        resultStream
      }
      else -> {
        resultStream.toBlocking().first()
      }
    }
  }

  fun handle(jo: JsonObject) {
    when {
      jo.containsKey("result") -> {
        val raw = jo.getValue("result")
        val result = Json.mapper.convertValue<Any>(raw, payloadType)
        resultStream.onNext(result)
      }
      jo.containsKey("error") -> {
        val error =jo.getJsonObject("error")
        onError(RuntimeException(error.encode()))
      }
      jo.containsKey("completed") -> resultStream.onCompleted()
    }
  }

  fun onError(err: Throwable) {
    resultStream.onError(err)
  }
}