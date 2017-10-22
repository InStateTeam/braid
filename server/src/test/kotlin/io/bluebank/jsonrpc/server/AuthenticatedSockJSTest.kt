package io.bluebank.jsonrpc.server

import io.bluebank.jsonrpc.server.socket.*
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions
import io.vertx.ext.auth.shiro.ShiroAuthRealmType
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.ext.web.handler.SessionHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.handler.sockjs.SockJSSocket
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
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
      JacksonKotlinInit.init()
      Vertx.vertx().deployVerticle(AuthenticatedSockJSTest())
    }

    private val logger = loggerFor<AuthenticatedSockJSTest>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss")
  }

  override fun start(startFuture: Future<Void>) {
    val router = Router.router(vertx)

    router.route().handler(CookieHandler.create())
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
//    setupAuth(router)
    setupSockJS(router)
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

  private fun setupSockJS(router: Router) {
    val sockJSHandler = SockJSHandler.create(vertx)
    sockJSHandler.socketHandler(this::socketHandler)
    router.route("/api/*").handler(sockJSHandler)
  }

  private fun socketHandler(socket: SockJSSocket) {
    val wrapper = SockJSWrapper(socket)
    val auth = AuthenticatedSocket(getAuthProvider())
    val transformer = TypedSocket(EchoRequest::class.java)
    wrapper.addListener(auth)
    auth.addListener(transformer)
    transformer.onData { this.write(it.str) }
  }

  private fun getAuthProvider(): ShiroAuth {
    val config = json {
      obj("properties_path" to "classpath:auth/shiro.properties")
    }
    return ShiroAuth.create(vertx, ShiroAuthOptions().setConfig(config).setType(ShiroAuthRealmType.PROPERTIES))
  }
}

data class EchoRequest(val str: String)

