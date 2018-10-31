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
package io.bluebank.braid.core.jsonrpc

import io.bluebank.braid.core.service.ConcreteServiceExecutor
import io.bluebank.braid.core.socket.AbstractSocket
import io.bluebank.braid.core.socket.NonBlockingSocket
import io.bluebank.braid.core.socket.Socket
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
    JsonRPCMounter(executor, vertx).apply { nonBlocking.addListener(this) }

    val async = context.async()
    socket.addResponseListener {
      println(Json.encode(it))
      async.complete()
    }
    socket.process(JsonRPCRequest(id = 1, method = "doSomething", params = listOf<Any>()))
    service.trigger()
  }

  private class MockSocket : AbstractSocket<JsonRPCRequest, JsonRPCResponse>() {
    private val responseListeners = mutableListOf<(JsonRPCResponse) -> Unit>()

    internal fun process(request: JsonRPCRequest) {
      onData(request)
    }

    internal fun addResponseListener(fn: (JsonRPCResponse) -> Unit) {
      responseListeners.add(fn)
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