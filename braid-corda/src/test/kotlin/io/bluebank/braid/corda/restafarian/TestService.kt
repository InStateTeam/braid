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

import io.bluebank.braid.corda.BraidConfig
import io.bluebank.braid.corda.BraidServer
import io.bluebank.braid.core.http.HttpServerConfig
import io.bluebank.braid.core.security.JWTUtils
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.HttpHeaderValues
import io.swagger.annotations.*
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTOptions
import io.vertx.ext.web.RoutingContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import javax.ws.rs.core.MediaType

class TestService {
  fun sayHello() = "hello"
  fun sayHelloAsync() = Future.succeededFuture<String>("hello, async!")
  fun echo(msg: String) = "echo: $msg"
  fun getBuffer(): Buffer = Buffer.buffer("hello")
  fun getByteArray(): ByteArray = Buffer.buffer("hello").bytes
  fun getByteBuf(): ByteBuf = Buffer.buffer("hello").byteBuf
  fun getByteBuffer(): ByteBuffer = Buffer.buffer("hello").byteBuf.nioBuffer()
  fun doubleBuffer(bytes: Buffer): Buffer =
    Buffer.buffer(bytes.length() * 2)
      .appendBytes(bytes.bytes)
      .appendBytes(bytes.bytes)

  @ApiOperation(value = "do something custom", response = String::class, consumes = MediaType.TEXT_PLAIN, produces = MediaType.TEXT_PLAIN)
  @ApiImplicitParams(ApiImplicitParam(name = "name", value = "name parameter", dataTypeClass = String::class, paramType = "path", defaultValue = "Margaret", required = true, examples = Example(ExampleProperty("Satoshi"), ExampleProperty("Margaret"), ExampleProperty("Alan"))))
  fun somethingCustom(rc: RoutingContext) {
    val name = rc.request().getParam("foo") ?: "Margaret"
    rc.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON).setChunked(true).end(Json.encode("Hello, $name!"))
  }
}

class TestServiceApp(port: Int, private val service: TestService) {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      TestServiceApp(8080, TestService())
    }
  }

  private val tempJKS = File.createTempFile("temp-", ".jceks")!!
  private val jwtSecret = "secret"
  private lateinit var jwtAuth: JWTAuth

  val server: BraidServer

  init {
    val thisObj = this
    server = BraidConfig()
      .withPort(port)
      .withService(service)
      .withAuthConstructor(this::createAuthProvider)
      .withHttpServerOptions(HttpServerConfig.defaultServerOptions().setSsl(false))
      .withRestConfig(RestConfig(serviceName = "my-service")
        .withAuthSchema(AuthSchema.Token)
        .withPaths {
          group("General Ledger") {
            unprotected {
              get("/hello-async", service::sayHelloAsync)
              post("/login", thisObj::login)
              get("/hello", service::sayHello)
              get("/buffer", service::getBuffer)
              get("/bytearray", service::getByteArray)
              get("/bytebuf", service::getByteBuf)
              get("/bytebuffer", service::getByteBuffer)
              post("/doublebuffer", service::doubleBuffer)
              post("/custom", service::somethingCustom)
            }
            protected {
              post("/echo", service::echo)
            }
          }
        })
      .bootstrapBraid(TestAppServiceHub())
  }

  fun whenReady(): Future<String> = server.whenReady()
  fun shutdown() = server.shutdown()

  @Suppress("MemberVisibilityCanBePrivate")
  fun login(request: LoginRequest): String {
    if (request == LoginRequest("sa", "admin")) {
      return jwtAuth.generateToken(JsonObject().put("user", request.user), JWTOptions().setExpiresInMinutes(24 * 60))
    } else {
      throw RuntimeException("failed to authenticate")
    }
  }

  private fun createAuthProvider(vertx: Vertx): AuthProvider {
    ensureJWTKeyStoreExists()
    return JWTAuth.create(vertx, JsonObject().put("keyStore", JsonObject()
      .put("path", tempJKS.absolutePath)
      .put("type", "jceks")
      .put("password", jwtSecret))).apply {
      jwtAuth = this
    }
  }

  private fun ensureJWTKeyStoreExists() {
    val ks = JWTUtils.createSimpleJWTKeyStore(jwtSecret)
    FileOutputStream(tempJKS.absoluteFile).use {
      ks.store(it, jwtSecret.toCharArray())
      it.flush()
    }
  }
}

data class LoginRequest(@ApiModelProperty(value = "user name", example = "sa") val user: String, @ApiModelProperty(value = "password", example = "admin") val password: String)

