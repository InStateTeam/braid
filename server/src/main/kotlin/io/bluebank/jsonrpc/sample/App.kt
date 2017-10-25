package io.bluebank.jsonrpc.sample

import io.bluebank.jsonrpc.server.JsonRPCServer
import io.vertx.core.Vertx
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions
import io.vertx.ext.auth.shiro.ShiroAuthRealmType
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj


fun main(args: Array<String>) {
  val vertx = Vertx.vertx()
  JsonRPCServer(
      vertx = vertx,
      services = listOf(CalculatorService(), AccountService()),
      authProvider = getAuthProvider(vertx)
  ).start()
}

private fun getAuthProvider(vertx: Vertx): ShiroAuth {
  val config = json {
    obj("properties_path" to "classpath:auth/shiro.properties")
  }
  return ShiroAuth.create(vertx, ShiroAuthOptions().setConfig(config).setType(ShiroAuthRealmType.PROPERTIES))
}