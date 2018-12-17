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
package io.bluebank.braid.core.http

import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.JksOptions
import java.io.ByteArrayOutputStream

class HttpServerConfig {
  companion object {
    @JvmStatic
    fun defaultServerOptions(): HttpServerOptions {
      val jksPath =
        HttpServerConfig::class.java.`package`.name.replace(".", "/") + "/default.jks"
      val jksBuffer = getResourceAsBuffer(jksPath)
      return HttpServerOptions()
        .setSsl(true)
        .setKeyStoreOptions(
          JksOptions()
            .setValue(jksBuffer)
            .setPassword("8a5500n")
        )
    }

    private fun getResourceAsBuffer(path: String): Buffer? {
      val jksStream = HttpServerConfig::class.java.classLoader.getResourceAsStream(path)
      val baos = ByteArrayOutputStream(jksStream.available())
      while (jksStream.available() > 0) {
        baos.write(jksStream.read())
      }
      val jksBytes = baos.toByteArray()!!
      return Buffer.buffer(jksBytes)
    }
  }
}
