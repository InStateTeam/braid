package io.bluebank.braid.core.jsonrpc

import io.bluebank.braid.core.service.ConcreteServiceExecutor
import io.bluebank.braid.core.socket.NonBlockingSocket
import io.bluebank.braid.core.socket.Socket
import io.bluebank.braid.core.socket.SocketListener
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.auth.User
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

class ControlledService {
  private val lock = Object()
  private val latch = CountDownLatch(1)
  internal fun trigger() {
    latch.countDown()
  }

  fun doSomething() {
    latch.await()
  }
}


@RunWith(VertxUnitRunner::class)
class JsonRPCMounterTest {
  private val vertx = Vertx.vertx()

  @Test
  fun `requests with duplicate ids to those in progress should throw an exception`(context: TestContext) {
    val service = ControlledService()
    val executor = ConcreteServiceExecutor(service)
    val socket = MockSocket()
    val nonBlocking = NonBlockingSocket<JsonRPCRequest, JsonRPCResponse>(vertx).apply { socket.addListener(this) }
    JsonRPCMounter(executor).apply { nonBlocking.addListener(this) }

    val async = context.async()
    socket.addResponseListener {
      println(Json.encode(it))
      async.complete()
    }
    socket.process(JsonRPCRequest(id = 1, method = "doSomething", params = listOf<Any>()))
    service.trigger()
  }

  private class MockSocket : Socket<JsonRPCRequest, JsonRPCResponse> {
    private val socketListeners = mutableListOf<SocketListener<JsonRPCRequest, JsonRPCResponse>>()
    private val responseListeners = mutableListOf<(JsonRPCResponse) -> Unit>()

    internal fun process(request: JsonRPCRequest) {
      socketListeners.forEach { it.dataHandler(this, request) }
    }

    internal fun addResponseListener(fn: (JsonRPCResponse) -> Unit) {
      responseListeners.add(fn)
    }

    override fun addListener(listener: SocketListener<JsonRPCRequest, JsonRPCResponse>): Socket<JsonRPCRequest, JsonRPCResponse> {
      socketListeners.add(listener)
      listener.onRegister(this)
      return this
    }

    override fun write(obj: JsonRPCResponse): Socket<JsonRPCRequest, JsonRPCResponse> {
      responseListeners.forEach { it(obj) }
      return this
    }

    override fun user(): User? {
      return null
    }
  }
}