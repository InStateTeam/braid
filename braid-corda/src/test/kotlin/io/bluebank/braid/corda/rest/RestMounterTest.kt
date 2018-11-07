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
package io.bluebank.braid.corda.rest

import io.bluebank.braid.core.socket.findFreePort
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpHeaders.CONTENT_LENGTH
import io.vertx.core.http.HttpHeaders.CONTENT_TYPE
import io.vertx.core.json.Json
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class RestMounterTest {
  private val port = findFreePort()
  private val service = TestServiceApp(port, TestService())

  @Before
  fun before(context: TestContext) {
    service.whenReady().setHandler(context.asyncAssertSuccess())
  }

  @After
  fun after() {
    service.shutdown()
  }

  @Test
  fun `test that we can mount rest endpoints and access via swagger`(context: TestContext) {
    val client = service.server.vertx.createHttpClient()
    val async1 = context.async()
    client.get(port, "localhost", "${TestServiceApp.SWAGGER_ROOT}/swagger.json")
      .exceptionHandler(context::fail)
      .handler {
        it.bodyHandler {
          it.toJsonObject()
          async1.complete()
        }
      }
      .end()

    val async2 = context.async()
    client.get(port, "localhost", "${TestServiceApp.SWAGGER_ROOT}/")
      .exceptionHandler(context::fail)
      .handler {
        it.bodyHandler {
          val body = it.toString()
          context.assertTrue(body.contains("<title>Swagger UI</title>", true))
          async2.complete()
        }
      }
      .end()

    val async3 = context.async()
    client.get(port, "localhost", "${TestServiceApp.REST_API_ROOT}/hello")
      .exceptionHandler(context::fail)
      .handler {
        it.bodyHandler {
          context.assertEquals("hello", it.toString())
          async3.complete()
        }
      }
      .end()

    val async4 = context.async()
    client.get(port, "localhost", "${TestServiceApp.REST_API_ROOT}/buffer")
      .exceptionHandler(context::fail)
      .handler { response ->
        response.bodyHandler { body ->
          context.assertEquals(APPLICATION_OCTET_STREAM.toString(), response.getHeader(HttpHeaders.CONTENT_TYPE))
          context.assertEquals("hello", body.toString())
          async4.complete()
        }
      }
      .end()


    val async5 = context.async()
    client.get(port, "localhost", "${TestServiceApp.REST_API_ROOT}/bytearray")
      .exceptionHandler(context::fail)
      .handler { response ->
        response.bodyHandler { body ->
          context.assertEquals(APPLICATION_OCTET_STREAM.toString(), response.getHeader(HttpHeaders.CONTENT_TYPE))
          context.assertEquals("hello", body.toString())
          async5.complete()
        }
      }
      .end()


    val async6 = context.async()
    client.get(port, "localhost", "${TestServiceApp.REST_API_ROOT}/bytebuf")
      .exceptionHandler(context::fail)
      .handler { response ->
        response.bodyHandler { body ->
          context.assertEquals(APPLICATION_OCTET_STREAM.toString(), response.getHeader(HttpHeaders.CONTENT_TYPE))
          context.assertEquals("hello", body.toString())
          async6.complete()
        }
      }
      .end()

    val async7 = context.async()
    client.get(port, "localhost", "${TestServiceApp.REST_API_ROOT}/bytebuffer")
      .exceptionHandler(context::fail)
      .handler { response ->
        response.bodyHandler { body ->
          context.assertEquals(APPLICATION_OCTET_STREAM.toString(), response.getHeader(HttpHeaders.CONTENT_TYPE))
          context.assertEquals("hello", body.toString())
          async7.complete()
        }
      }
      .end()

    val async8 = context.async()
    val bytes = Buffer.buffer("hello")
    client.post(port, "localhost", "${TestServiceApp.REST_API_ROOT}/doublebuffer")
      .putHeader(CONTENT_TYPE, HttpHeaderValues.APPLICATION_OCTET_STREAM)
      .putHeader(CONTENT_LENGTH, bytes.length().toString())
      .exceptionHandler(context::fail)
      .handler { response ->
        context.assertEquals(2, response.statusCode() / 100)
        response.bodyHandler { body ->
          context.assertEquals(APPLICATION_OCTET_STREAM.toString(), response.getHeader(HttpHeaders.CONTENT_TYPE))
          context.assertEquals("hellohello", body.toString())
          async8.complete()
        }
      }
      .end(bytes)

    val async9 = context.async()
    var token = ""
    client.post(port, "localhost", "${TestServiceApp.REST_API_ROOT}/login")
      .exceptionHandler(context::fail)
      .handler { response ->
        response.bodyHandler { body ->
          token = body.toString()
          async9.complete()
        }
      }
      .setChunked(true)
      .end(Json.encode(LoginRequest(user = "sa", password = "admin")))

    async9.await()

    val async10 = context.async()
    client.post(port, "localhost", "${TestServiceApp.REST_API_ROOT}/echo")
      .exceptionHandler(context::fail)
      .handler { response ->
        response.bodyHandler { body ->
          context.assertEquals("echo: hello", body.toString())
          async10.complete()
        }
      }
      .putHeader("Authorization", "Bearer $token")
      .setChunked(true)
      .end("hello")
  }

  @Test
  fun testThatMethodThatFailsReturnsAllHeaders(context: TestContext) {
    val client = service.server.vertx.createHttpClient()
    val async1 = context.async()
    client.get(port, "localhost", "${TestServiceApp.REST_API_ROOT}/willfail")
      .exceptionHandler(context::fail)
      .handler {
        context.assertEquals(500, it.statusCode())
        context.assertEquals("total fail!", it.statusMessage())
        val expectedContentType = "text/plain; charset=utf8"
        context.assertEquals(expectedContentType, it.getHeader(CONTENT_TYPE))

        it.bodyHandler {
          val body = it.toString("utf8")
          context.assertEquals("total fail!", body)
          async1.complete()
        }
      }
      .end()
  }
}

