package io.bluebank.jsonrpc.server

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions
import io.vertx.ext.auth.shiro.ShiroAuthRealmType
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BasicAuthHandler
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.handler.UserSessionHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

class AuthenticatedSockJSTest : AbstractVerticle() {
  override fun start(startFuture: Future<Void>?) {
    val config = json {
      obj("properties_path" to "classpath:login/shiro.properties")
    }
    val provider = ShiroAuth.create(vertx, ShiroAuthOptions().setConfig(config).setType(ShiroAuthRealmType.PROPERTIES))
    val router = Router.router(vertx)

    router.route().handler(CookieHandler.create())
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
    router.route().handler(UserSessionHandler.create(provider))
    router.route("/api/services/*").handler(BasicAuthHandler.create(provider))
    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(80)
    super.start(startFuture)
  }
}