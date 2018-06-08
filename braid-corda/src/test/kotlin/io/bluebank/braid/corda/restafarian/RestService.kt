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

import io.bluebank.braid.corda.restafarian.Restafarian.Companion.mount
import io.bluebank.braid.corda.router.Routers
import io.netty.buffer.ByteBuf
import io.swagger.models.Scheme
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import java.nio.ByteBuffer

class MyService {
  fun sayHello() = "hello"
  fun echo(msg: String) = "echo: $msg"
  fun getBuffer(): Buffer = Buffer.buffer("hello")
  fun getByteArray(): ByteArray = Buffer.buffer("hello").bytes
  fun getByteBuf(): ByteBuf = Buffer.buffer("hello").byteBuf
  fun getByteBuffer(): ByteBuffer = Buffer.buffer("hello").byteBuf.nioBuffer()
  fun doubleBuffer(bytes: Buffer): Buffer =
    Buffer.buffer(bytes.length() * 2)
      .appendBytes(bytes.bytes)
      .appendBytes(bytes.bytes)

}

class MyServiceSetup(vertx: Vertx, port: Int, service: MyService) {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      MyServiceSetup(Vertx.vertx(), 8080, MyService())
    }
  }

  init {
    val router = Routers.create(vertx, port)
    mount(
      serviceName = "my-service",
      hostAndPortUri = "http://localhost:$port",
      apiPath = "/api",
      router = router,
      scheme = Scheme.HTTP,
      vertx = vertx
    ) {
      this.group("General Ledger") {
        this.get("/hello", service::sayHello)
        this.post("/echo", service::echo)
        this.get("/buffer", service::getBuffer)
        this.get("/bytearray", service::getByteArray)
        this.get("/bytebuf", service::getByteBuf)
        this.get("/bytebuffer", service::getByteBuffer)
        this.post("/doublebuffer", service::doubleBuffer)
      }
    }
    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(port)
  }
}