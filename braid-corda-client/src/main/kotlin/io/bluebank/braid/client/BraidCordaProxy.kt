package io.bluebank.braid.client

import io.bluebank.braid.corda.serialisation.BraidCordaJacksonInit
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
import java.util.concurrent.atomic.AtomicLong


class BraidCordaProxyClient(config: BraidClientConfig, vertx: Vertx) : BraidProxyClient(config, vertx)  {

  companion object {
    init {
      BraidCordaJacksonInit.init()
    }
  }

}