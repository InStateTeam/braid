package io.bluebank.braid.integration.server

import io.bluebank.braid.server.JsonRPCServer
import io.bluebank.braid.server.JsonRPCServerBuilder.Companion.createServerBuilder
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions
import io.vertx.ext.auth.shiro.ShiroAuthRealmType
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

class TestServer(private val braidPort: Int) {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      val server = TestServer(8080)
      server.start { }
    }
  }
  private val vertx = Vertx.vertx()
  private lateinit var braidServer: JsonRPCServer

  fun start(callback: (AsyncResult<Void>) -> Unit) {
    braidServer = createServerBuilder()
        .withVertx(vertx)
        .withPort(braidPort)
        .withService(MyService(vertx))
        .withAuthProvider(getAuthProvider(vertx))
        .build()
    braidServer.start(callback)
  }

  fun stop() {
    braidServer.stop {
      vertx.close {}
    }
  }

  private fun getAuthProvider(vertx: Vertx): ShiroAuth {
    val config = json {
      obj("properties_path" to "classpath:auth/shiro.properties")
    }
    return ShiroAuth.create(vertx, ShiroAuthOptions().setConfig(config).setType(ShiroAuthRealmType.PROPERTIES))
  }
}

