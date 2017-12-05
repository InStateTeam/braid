package io.bluebank.hermes.core.http

import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.JksOptions
import java.io.ByteArrayOutputStream

class HttpServerConfig {
  companion object {
    @JvmStatic
    fun defaultServerOptions() : HttpServerOptions {
      val jksPath = HttpServerConfig::class.java.`package`.name.replace(".", "/") + "/default.jks"
      val jksBuffer = getResourceAsBuffer(jksPath)
      return HttpServerOptions()
          .setSsl(true)
          .setKeyStoreOptions(
              JksOptions()
                  .setValue(jksBuffer)
                  .setPassword("8a5500n"))
    }

    private fun getResourceAsBuffer(path: String): Buffer? {
      val jksStream = HttpServerConfig::class.java.classLoader.getResourceAsStream(path)
      val baos = ByteArrayOutputStream(jksStream.available())
      while (jksStream.available() > 0) {
        baos.write(jksStream.read())
      }
      val jksBytes = baos.toByteArray()!!
      val jksBuffer = Buffer.buffer(jksBytes)
      return jksBuffer
    }
  }
}
