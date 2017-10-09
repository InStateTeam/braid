package io.bluebank.jsonrpc.server

import io.vertx.core.*
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.ServerWebSocket
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler

class JsonRPCServer(val rootPath: String, val port: Int = 8080, val services: List<Any>) {
  private var vertx: Vertx? = null

  init {
    JacksonKotlinInit.init()
    if (!rootPath.endsWith("/")) {
      throw RuntimeException("path must end with '/': $rootPath")
    }
  }

  fun start() {
    start { }
  }

  fun start(callback: (AsyncResult<Void>) -> Unit) {
    if (vertx == null) {
      val vertx = Vertx.vertx()
      vertx.deployVerticle(App(rootPath, services, port)) {
        if (it.failed()) {
          println("failed to deploy: ${it.cause().message}")
          callback(Future.failedFuture(it.cause()))
        } else {
          callback(Future.succeededFuture())
        }
      }
      this.vertx = vertx
    }
  }

  fun stop() {
    stop {}
  }

  fun stop(callback: (AsyncResult<Void>) -> Unit) {
    if (vertx != null) {
      vertx!!.close {
        vertx = null
        callback(it)
      }
    }
  }


  class App(val rootPath: String, val services: List<Any>, val port: Int) : AbstractVerticle() {
    companion object {
      val logger = loggerFor<App>()
    }

    val serviceMap: MutableMap<String, ServiceExecutor> by lazy {
      services.map { getServiceName(it) to wrapConcreteService(it) }.toMap().toMutableMap()
    }

    override fun start(startFuture: Future<Void>) {
      val router = setupRouter()
      setupWebserver(router, startFuture)
    }

    private fun wrapConcreteService(service: Any) : ServiceExecutor {
      return CompositeExecutor(ConcreteServiceExecutor(service), JavascriptExecutor(vertx, getServiceName(service)))
    }


    private fun ServiceExecutor.getJavascriptExecutor() : JavascriptExecutor {
      return when (this) {
        is CompositeExecutor -> {
          executors
            .filter { it is JavascriptExecutor }
            .map { it as JavascriptExecutor }
            .firstOrNull() ?: throw RuntimeException("cannot find javascript executor")
        }
        is JavascriptExecutor -> this
        else -> {
          throw RuntimeException("found executor is not a ${JavascriptExecutor::class.simpleName} or doesn't contain one")
        }
      }
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
      router.get("/api/services").handler { it.getServiceList() }
      router.get("/api/services/:serviceId/script").handler { it.getServiceScript(it.pathParam("serviceId")) }
      router.post("/api/services/:serviceId/script").handler { it.saveServiceScript(it.pathParam("serviceId"), it.bodyAsString)}
      router.get().handler(
        StaticHandler.create("editor-web")
          .setCachingEnabled(false)
          .setMaxCacheSize(1)
          .setCacheEntryTimeout(1)
      )
      return router
    }

    private fun RoutingContext.getServiceList() {
      write(serviceMap.keys)
    }

    private fun RoutingContext.getServiceScript(serviceName: String) {
      val script = getJavascriptExecutorForService(serviceName).getScript()

      response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "text/javascript")
        .putHeader(HttpHeaders.CONTENT_LENGTH, script.length().toString())
        .end()
    }

    private fun getJavascriptExecutorForService(serviceName: String): JavascriptExecutor {
      return serviceMap.computeIfAbsent(serviceName) {
        JavascriptExecutor(vertx, serviceName)
      }.getJavascriptExecutor()
    }

    private fun RoutingContext.saveServiceScript(serviceName: String, script: String) {
      val service = getJavascriptExecutorForService(serviceName)
      try {
        service.updateScript(script)
        response().end()
      } catch(err: Throwable) {
        write(err)
      }
    }

    private fun onSocket(socket: ServerWebSocket) {
      with(socket.path()) {
        if (!startsWith(rootPath)) {
          socket.reject()
        } else {
          val serviceName = drop(rootPath.length)
          val service = serviceMap[serviceName]
          if (service != null)
            JsonRPCMounter(service, socket)
        }
      }
    }

    private fun getServiceName(service: Any): String {
      return service.javaClass.getDeclaredAnnotation(JsonRPCService::class.java)?.name ?: service.javaClass.name.toLowerCase()
    }
  }

}

