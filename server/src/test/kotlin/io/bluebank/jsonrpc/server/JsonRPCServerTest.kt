package io.bluebank.jsonrpc.server

import io.bluebank.jsonrpc.server.JsonRPCServerBuilder.Companion.createServerBuilder
import io.bluebank.jsonrpc.server.services.impl.JavascriptExecutor
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.WebSocketFrame
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket

@RunWith(VertxUnitRunner::class)
class JsonRPCServerTest {
  private val port = getFreePort()
  private val vertx: Vertx = Vertx.vertx(VertxOptions().setBlockedThreadCheckInterval(30_000))
  private val server = createServerBuilder()
      .withVertx(vertx)
      .withPort(port)
      .build()
  private val client = vertx.createHttpClient(HttpClientOptions().setDefaultPort(port).setDefaultHost("localhost"))!!

  @Before
  fun before(testContext: TestContext) {
    JavascriptExecutor.clearScriptsFolder(vertx)
        .compose {
          val result = future<Void>()
          server.start(result.completer()::handle)
          result
        }.setHandler(testContext.asyncAssertSuccess())
  }

  @After
  fun after(testContext: TestContext) {
    JavascriptExecutor.clearScriptsFolder(vertx).setHandler(testContext.asyncAssertSuccess())
    server.stop(testContext.asyncAssertSuccess<Void>()::handle)
    client.close()
  }

  @Test
  fun `that we can list services and create them`(testContext: TestContext) {
    httpGetAsJsonArray("/api/services")
        .compose {
          testContext.assertEquals(0, it.size())
          httpGet("/api/services/myservice/script")
        }
        .map {
          testContext.assertEquals("", it.toString())
        }
        .compose {
          httpGetAsJsonArray("/api/services")
        }
        .map {
          testContext.assertEquals(1, it.size())
          testContext.assertEquals("myservice", it.getString(0))
        }
        .setHandler(testContext.asyncAssertSuccess())
  }

  @Test
  fun `that we can set a script on a service and execute it`(testContext: TestContext) {
    val script = """
      |// This is a comment
      |function add(lhs, rhs) {
      |  return lhs + rhs
      |}
      """.trimIndent().trimMargin("|")

    httpPost("/api/services/myservice/script", script)
        .compose {
          jsonRPC("ws://localhost:$port/api/jsonrpc/myservice/websocket", "add", 1, 2)
        }
        .map {
          Json.decodeValue(it, JsonRPCResultResponse::class.java)
        }
        .map { it.result.toString().toDouble().toInt() }
        .map {
          testContext.assertEquals(3, it)
        }
        .setHandler(testContext.asyncAssertSuccess())
  }

  private fun jsonRPC(url: String, method: String, vararg params: Any?): Future<Buffer> {
    val id = 1L
    val result = future<Buffer>()
    try {
      client.websocket(url) { socket ->
        socket.handler { response ->
          val jo = JsonObject(response)
          val responseId = jo.getLong("id")
          if (responseId != id) {
            result.fail("expected id $id but $responseId")
          } else if (jo.containsKey("result")) {
            result.complete(response)
          } else if (jo.containsKey("error")) {
            result.fail(jo.getJsonObject("error").encode())
          } else if (jo.containsKey("completed")) {
            // we ignore the 'completed' message
          }
        }.exceptionHandler { err ->
          result.fail(err)
        }
        val request = JsonRPCRequest(id = id, method = method, params = params.toList())
        socket.writeFrame(WebSocketFrame.textFrame(Json.encode(request), true))
      }
    } catch (err: Throwable) {
      result.fail(err)
    }
    return result
  }

  private fun httpGetAsJsonArray(url: String): Future<JsonArray> {
    return httpGet(url).map { JsonArray(it) }
  }

  private fun httpGet(url: String): Future<Buffer> {
    val future = future<Buffer>()
    try {
      client.getNow(url) { response ->
        if (response.failed) {
          future.fail(response.statusMessage() ?: "failed")
        } else {
          response.bodyHandler {
            future.complete(it)
          }
        }
      }
    } catch (err: Throwable) {
      future.fail(err)
    }
    return future
  }

  private fun httpPost(url: String, data: String): Future<Buffer> {
    val future = future<Buffer>()
    try {
      client.post(url)
          .putHeader(HttpHeaders.CONTENT_LENGTH, data.length.toString())
          .handler {
            if ((it.statusCode() / 100) != 2) {
              future.fail("failed: ${it.statusMessage()}")
            } else {
              it.bodyHandler {
                future.complete(it)
              }
            }
          }
          .end(data)
    } catch (err: Throwable) {
      future.fail(err)
    }
    return future
  }

  private fun getFreePort(): Int {
    return (ServerSocket(0)).use {
      it.localPort
    }
  }
}