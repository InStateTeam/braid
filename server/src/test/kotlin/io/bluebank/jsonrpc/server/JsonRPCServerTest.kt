package io.bluebank.jsonrpc.server

import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
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
    server.start(testContext.asyncAssertSuccess<Void>()::handle)
  }

  @After
  fun after(testContext: TestContext) {
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

  private fun httpGetAsJsonArray(url: String) : Future<JsonArray> {
    return httpGet(url).map { JsonArray(it) }
  }

  private fun httpGet(url: String) : Future<Buffer> {
    val servicesFuture = future<Buffer>()
    try {
      client.getNow(url) { response ->
        response.bodyHandler {
          servicesFuture.complete(it)
        }
      }
    } catch (err: Throwable) {
      servicesFuture.fail(err)
    }
    return servicesFuture
  }

  private fun getFreePort(): Int {
    return (ServerSocket(0)).use {
      it.localPort
    }
  }
}