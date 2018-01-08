package io.bluebank.braid.client

import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocket
import io.vertx.core.http.WebSocketFrame
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import org.slf4j.Logger
import rx.Observable
import rx.Observer
import rx.Subscriber
import java.io.Closeable
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
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
    val result = Future.future<ServiceType>()
    val url = URL("ws", config.serviceURI.host, config.serviceURI.port, config.serviceURI.path)
    client.websocket(url.toString(), { socket ->
      val proxy = Proxy.newProxyInstance(clazz.classLoader, arrayOf(clazz), ProxyInvocationHandler(clazz, socket)) as ServiceType
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

private class ProxyInvocationHandler<T : Any>(private val clazz: Class<T>, private val socket: WebSocket, private val ) : InvocationHandler {
  private val nextId = AtomicLong(1)
  private val subscribers = mutableMapOf<Long, Observer<Buffer>>()

  companion object {
    private val log : Logger = loggerFor<ProxyInvocationHandler<*>>()
  }

  init {
    socket.handler(this::handler)
    socket.exceptionHandler(this::exceptionHandler)
  }

  override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any {

    val request = JsonRPCRequest(id = nextId.getAndIncrement(), method = method.name, params = args.toList())
  }

  private fun handler(buffer: Buffer) {
    try {
      val jo = JsonObject(buffer)
      val responseId = jo.getLong("id")
      when {
        responseId == null -> log.error("received response without id {}", buffer.toString())
        !subscribers.containsKey(responseId) -> log.error("no subscriber found for response id {}", responseId)
        jo.containsKey("result") -> subscribers[responseId]!!.onNext(buffer)
        jo.containsKey("error") -> subscribers[responseId]!!.onError(RuntimeException(jo.getJsonObject("error").encode()))
        jo.containsKey("completed") -> subscribers[responseId]!!.onCompleted()
      }
    } catch (err: Throwable) {
      log.error("failed to handle response message", err)
    }
  }

  private fun exceptionHandler(error: Throwable) {
    log.error("exception from socket", error)
    // TODO: handle retries?
  }

  private fun jsonRPC(url: String, method: String, vararg params: Any?): Observable<Buffer> {
    return Observable.create { subscriber ->
      val id = nextId.incrementAndGet()
      subscribers.put(id, Observer)
      try {
        val request = JsonRPCRequest(id = id, method = method, params = params.toList())
        socket.writeFrame(WebSocketFrame.textFrame(Json.encode(request), true))
      } catch (err: Throwable) {
        onRequestError(id, err)
      }
    }
  }

  private fun onRequestError(id: Long, err: Throwable) {
    val subscriber = subscribers[id]
    if (subscriber != null) {
      subscriber.onError(err)
      subscribers.remove(id)
    } else {
      // TODO: warn
    }
  }
}