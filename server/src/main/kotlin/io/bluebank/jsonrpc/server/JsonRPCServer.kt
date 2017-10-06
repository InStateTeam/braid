package io.bluebank.jsonrpc.server

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.ServerWebSocket
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import org.slf4j.LoggerFactory

class JsonRPCServer(val rootPath: String, val port: Int = 8080, val services: List<Any>) {
  private var vertx: Vertx? = null
  private val serviceMap: Map<String, Any> by lazy {
    services.map {
      getServicePath(it) to it
    }.toMap()
  }

  init {
    JacksonKotlinInit.init()
    if (!rootPath.endsWith("/")) {
      throw RuntimeException("path must end with '/': $rootPath")
    }
  }

  fun start() {
    if (vertx == null) {
      val vertx = Vertx.vertx()
      vertx.deployVerticle(App(serviceMap, port)) {
        if (it.failed()) {
          println("failed to deploy: ${it.cause().message}")
        }
      }
      this.vertx = vertx
    }
  }

  fun stop() {
    if (vertx != null) {
      vertx!!.close()
      vertx = null
    }
  }

  class App(val serviceMap: Map<String, Any>, val port: Int) : AbstractVerticle() {
    companion object {
      val logger = loggerFor<App>()
    }
    override fun start(startFuture: Future<Void>) {
      val router = setupRouter()
      setupWebserver(router, startFuture)
    }

    private fun setupWebserver(router: Router, startFuture: Future<Void>) {
      vertx.createHttpServer(HttpServerOptions().withCompatibleWebsockets())
        .websocketHandler(this::onSocket)
        .requestHandler(router::accept)
        .listen(port) {
          if (it.succeeded()) {
            logger.info("started on port $port")
            startFuture.complete()
          } else {
            logger.error("failed to start because", it.cause())
            startFuture.fail(it.cause())
          }
        }
    }

    private fun HttpServerOptions.withCompatibleWebsockets(): HttpServerOptions {
      this.websocketSubProtocols = "undefined"
      return this
    }

    private fun setupRouter(): Router {
      val router = Router.router(vertx)
      router.route().handler(BodyHandler.create())
      router.get().handler(
        StaticHandler.create("editor-web")
          .setCachingEnabled(false)
          .setMaxCacheSize(1)
          .setCacheEntryTimeout(1)
      )
      return router
    }

    private fun onSocket(socket: ServerWebSocket) {
      val service = serviceMap[socket.path()]
      if (service != null) {
        JsonRPCMounter(service, socket)
      } else {
        socket.reject()
      }
    }
  }

  private fun getServicePath(service: Any): String {
    val name = service.javaClass.getDeclaredAnnotation(JsonRPCService::class.java)?.name ?: service.javaClass.name.toLowerCase()
    return rootPath + name
  }
}