package io.bluebank.braid.client.invocations.impl

import io.bluebank.braid.client.BraidClientConfig
import io.bluebank.braid.client.invocations.Invocations
import io.bluebank.braid.core.jsonrpc.JsonRPCResultResponse
import io.bluebank.braid.core.socket.findFreePort
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.json.Json
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ConnectException
import java.net.URI
import kotlin.test.assertFailsWith

@RunWith(VertxUnitRunner::class)
class InvocationsImplTest {
  private val port = findFreePort()
  private val vertx = Vertx.vertx()
  private var server: HttpServer? = null
  private var invocations: Invocations? = null

  @After
  fun after(context: TestContext) {
    val async = context.async()
    server?.close {
      invocations?.close()
      vertx.close {
        async.complete()
      }
    } ?: vertx.close {
      async.complete()
    }
  }

  @Test
  fun `that sending a malformed request does not break the flow`(context: TestContext) {
    server = vertx.createHttpServer()
      .websocketHandler { socket ->
        socket.handler {
          socket.writeFinalTextFrame("&&&&&&&&&&")
          socket.writeFinalTextFrame(Json.encode(JsonRPCResultResponse(id = 1, result = "hello")))
        }
      }.listen(port)
    invocations = Invocations.create(vertx = vertx,
      config = BraidClientConfig(URI("http://localhost:$port/api"), tls = false),
      exceptionHandler = {
        context.fail(it)
      },
      closeHandler = {
      })
    val result = invocations?.invoke("foo", String::class.java, arrayOf()) as String
    context.assertEquals("hello", result)
  }

  @Test
  fun `that trying to connect to a non existent uri fails`(context: TestContext) {
    assertFailsWith<ConnectException> {
      Invocations.create(vertx = vertx,
        config = BraidClientConfig(URI("http://localhost:$port/api"), tls = false),
        exceptionHandler = {
          context.fail(it)
        },
        closeHandler = {
        })
    }
  }
}