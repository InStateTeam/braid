package io.bluebank.hermes.sample

import io.bluebank.hermes.server.*
import io.bluebank.hermes.server.services.impl.ConcreteServiceExecutor
import io.bluebank.hermes.server.socket.AuthenticatedSocket
import io.bluebank.hermes.server.socket.SockJSSocketWrapper
import io.bluebank.hermes.server.socket.TypedSocket
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

/**
 * Not an automated test.
 * Demonstrates the principles of a secure eventbus over sockjs
 */
class AuthenticatedSockJSTest : AbstractVerticle() {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory")
      JacksonKotlinInit.init()
      Vertx.vertx().deployVerticle(AuthenticatedSockJSTest())
    }

    private val logger = loggerFor<AuthenticatedSockJSTest>()
  }

  override fun start(startFuture: Future<Void>) {
    val router = Router.router(vertx)

    router.route().handler(CookieHandler.create())
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
    val timeService = setupTimeService()
    setupSockJS(router, timeService)
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

  private fun setupTimeService(): TimeService {
    return TimeService(vertx)
  }

  private fun setupStatic(router: Router) {
    router.get().handler(StaticHandler.create("streamingtest")
//        .setCachingEnabled(false)
//        .setCacheEntryTimeout(1).setMaxCacheSize(1)
    // enable the above lines to turn off caching - suitable for rapid coding of the UI
    )
  }

  private fun setupSockJS(router: Router, timeService: TimeService) {
    val sockJSHandler = SockJSHandler.create(vertx)
    sockJSHandler.socketHandler { socketHandler(it, timeService) }
    router.route("/api/*").handler(sockJSHandler)
  }

  private fun socketHandler(socket: SockJSSocket, timeService: TimeService) {
    val wrapper = SockJSSocketWrapper.create(socket)
    val auth = AuthenticatedSocket.create(getAuthProvider())
    val mount = JsonRPCMounter(ConcreteServiceExecutor(timeService))
    val transformer = TypedSocket.create<JsonRPCRequest, JsonRPCResponse>()
    wrapper.addListener(auth)
    auth.addListener(transformer)
    transformer.addListener(mount)
  }

  private fun getAuthProvider(): ShiroAuth {
    val config = json {
      obj("properties_path" to "classpath:auth/shiro.properties")
    }
    return ShiroAuth.create(vertx, ShiroAuthOptions().setConfig(config).setType(ShiroAuthRealmType.PROPERTIES))
  }
}


