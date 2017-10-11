package io.bluebank.jsonrpc.server

import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket

@RunWith(VertxUnitRunner::class)
class JsonRPCServerTest {
  val port = getFreePort()
  val server = JsonRPCServer("/api/", port, listOf()) // we're just going to use
  val vertx = Vertx.vertx()
  val client = vertx.createHttpClient(HttpClientOptions().setDefaultPort(port).setDefaultHost("localhost"))

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
          jsonRPC("ws://localhost:$port/api/myservice", "add", 1, 2)
        }
        .map {
          Json.decodeValue(it, JsonRPCResponsePayload::class.java)
        }
        .map { it.result.toString().toDouble().toInt() }
        .map {
          testContext.assertEquals(3, it)
        }
        .setHandler(testContext.asyncAssertSuccess())
  }
  
  private fun jsonRPC(url: String, method: String, vararg params: Any?) : Future<Buffer> {
    val result = future<Buffer>()
    try {
      client.websocket(url) { socket ->
        socket.handler { response ->
          // TODO: check response has correct id
          result.complete(response)
          socket.close()
        }.exceptionHandler { err ->
          result.fail(err)
          socket.close()
        }
        val request = JsonRPCRequest(id = 1, method = method, params = params.toList())
        socket.writeFinalTextFrame(Json.encode(request))
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