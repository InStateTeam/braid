package io.bluebank.jsonrpc

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler

fun main(args: Array<String>) {
  with(KotlinModule()) {
    Json.mapper.registerModule(this)
    Json.prettyMapper.registerModule(this)
  }
  val vertx = Vertx.vertx()
  vertx.deployVerticle(App())
}

class App : AbstractVerticle() {
  override fun start() {
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    vertx.createHttpServer()
      .websocketHandler(this::onSocket)
      .requestHandler(router::accept)
      .listen(8080)
  }

  private fun onSocket(socket: ServerWebSocket) {
    if (socket.path() == "/api/myservice") {
      JsonRPCMounter(MyService(), socket)
    }
  }
}

