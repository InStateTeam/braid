package io.bluebank.jsonrpc.server

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions
import io.vertx.ext.auth.shiro.ShiroAuthRealmType
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.web.handler.sockjs.BridgeOptions
import io.vertx.kotlin.ext.web.handler.sockjs.PermittedOptions
import java.text.SimpleDateFormat
import java.util.*

/**
 * Not an automated test.
 * Demonstrates the principles of a secure eventbus over sockjs
 */
class AuthenticatedSockJSTest : AbstractVerticle() {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      Vertx.vertx().deployVerticle(AuthenticatedSockJSTest())
    }

    private val logger = loggerFor<AuthenticatedSockJSTest>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss")
  }

  override fun start(startFuture: Future<Void>) {
    val router = Router.router(vertx)

    router.route().handler(CookieHandler.create())
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
    setupAuth(router)
    setupEventbusBridge(router)
    setupTimeService()
    setupStatic(router)

    val PORT = 8080
    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(PORT) {
        if (it.succeeded()) {
          logger.info("started on http://localhost:$PORT")
        } else {
          logger.error("failed to startup", it.cause())
        }
        startFuture.completer().handle(it.mapEmpty<Void>())
      }

  }

  private fun setupTimeService() {
    vertx.setPeriodic(1000) {
      vertx.eventBus().publish("time", timeFormat.format(Date()))
    }
  }

  private fun setupStatic(router: Router) {
    router.get().handler(StaticHandler.create("eventbus-test").setCachingEnabled(false).setCacheEntryTimeout(1).setMaxCacheSize(1))
  }

  private fun setupEventbusBridge(router: Router) {
    val sockJSHandler = SockJSHandler.create(vertx)
    val outboundPermitted = PermittedOptions(address = "time")
    val options = BridgeOptions(outboundPermitted = listOf(outboundPermitted))
    sockJSHandler.bridge(options)
    router.route("/eventbus/*").handler(sockJSHandler)
  }

  private fun setupAuth(router: Router) {
    val config = json {
      obj("properties_path" to "classpath:login/shiro.properties")
    }
    val provider = ShiroAuth.create(vertx, ShiroAuthOptions().setConfig(config).setType(ShiroAuthRealmType.PROPERTIES))
    router.route().handler(UserSessionHandler.create(provider))
    router.route("/eventbus/*").handler(BasicAuthHandler.create(provider))
  }
}