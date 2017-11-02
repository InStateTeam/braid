package io.bluebank.hermes.sample

import io.bluebank.hermes.server.JsonRPCServerBuilder.Companion.createServerBuilder
import io.vertx.core.Vertx
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions
import io.vertx.ext.auth.shiro.ShiroAuthRealmType
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj


fun main(args: Array<String>) {
  val vertx = Vertx.vertx()
  val server = createServerBuilder()
      .withVertx(vertx)
      .withService(CalculatorService())
      .withService(TimeService(vertx))
      .withAuthProvider(getAuthProvider(vertx))
      .build()

  server.start()
}

/**
 * We could use any auth provider
 */
private fun getAuthProvider(vertx: Vertx): ShiroAuth {
  val config = json {
    obj("properties_path" to "classpath:auth/shiro.properties")
  }
  return ShiroAuth.create(vertx, ShiroAuthOptions().setConfig(config).setType(ShiroAuthRealmType.PROPERTIES))
}