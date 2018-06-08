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
package io.bluebank.braid.corda.restafarian

import io.bluebank.braid.core.socket.findFreePort
import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Vertx
import io.vertx.core.http.HttpHeaders
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class RestafarianTest {
  private val port = findFreePort()
  private lateinit var vertx: Vertx
  private val service = MyService()
  @Before
  fun before() {
    vertx = Vertx.vertx()
  }

  @After
  fun after(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
  }

  @Test
  fun `test that we can mount restafarian and access via swagger`(context: TestContext) {
    MyServiceSetup(vertx, port, MyService())
    val client = vertx.createHttpClient()
    val async1 = context.async()
    client.get(port, "localhost", "/swagger.json")
        .exceptionHandler(context::fail)
        .handler {
          it.bodyHandler {
            it.toJsonObject()
            async1.complete()
          }
        }
        .end()

    val async2 = context.async()
    client.get(port, "localhost", "/")
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
    client.get(port, "localhost", "/api/hello")
        .exceptionHandler(context::fail)
        .handler {
          it.bodyHandler {
            context.assertEquals("hello", it.toString())
            async3.complete()
          }
        }
        .end()

    val async4 = context.async()
    client.get(port, "localhost", "/api/buffer")
      .exceptionHandler(context::fail)
      .handler { response ->
        response.bodyHandler { body ->
          context.assertEquals(HttpHeaderValues.APPLICATION_OCTET_STREAM.toString(), response.getHeader(HttpHeaders.CONTENT_TYPE))
          context.assertEquals("hello", body.toString())
          async4.complete()
        }
      }
      .end()


    val async5 = context.async()
    client.get(port, "localhost", "/api/bytearray")
      .exceptionHandler(context::fail)
      .handler { response ->
        response.bodyHandler { body ->
          context.assertEquals(HttpHeaderValues.APPLICATION_OCTET_STREAM.toString(), response.getHeader(HttpHeaders.CONTENT_TYPE))
          context.assertEquals("hello", body.toString())
          async5.complete()
        }
      }
      .end()


    val async6 = context.async()
    client.get(port, "localhost", "/api/bytebuf")
      .exceptionHandler(context::fail)
      .handler { response ->
        response.bodyHandler { body ->
          context.assertEquals(HttpHeaderValues.APPLICATION_OCTET_STREAM.toString(), response.getHeader(HttpHeaders.CONTENT_TYPE))
          context.assertEquals("hello", body.toString())
          async6.complete()
        }
      }
      .end()

    val async7 = context.async()
    client.get(port, "localhost", "/api/bytebuffer")
      .exceptionHandler(context::fail)
      .handler { response ->
        response.bodyHandler { body ->
          context.assertEquals(HttpHeaderValues.APPLICATION_OCTET_STREAM.toString(), response.getHeader(HttpHeaders.CONTENT_TYPE))
          context.assertEquals("hello", body.toString())
          async7.complete()
        }
      }
      .end()
  }
}

