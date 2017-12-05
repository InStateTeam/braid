package io.bluebank.hermes.corda

import io.bluebank.hermes.core.http.setupAllowAnyCORS
import io.bluebank.hermes.core.http.withCompatibleWebsockets
import io.bluebank.hermes.core.logging.loggerFor
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import net.corda.node.internal.Node
import net.corda.node.services.api.ServiceHubInternal

class HermesVerticle(private val services: ServiceHubInternal, private val config: HermesConfig) : AbstractVerticle() {
  companion object {
    private val log = loggerFor<HermesVerticle>()
  }
  override fun start(startFuture: Future<Void>?) {
    log.info("starting with ", config)
    val router = setupRouter()
    setupWebserver(router)
    Node.printBasicNodeInfo("Hermes server started on", "http://localhost:${config.port}${config.rootPath}")
  }

  private fun setupWebserver(router: Router) {
    vertx.createHttpServer(config.httpServerOptions.withCompatibleWebsockets())
        .requestHandler(router::accept)
        .listen(config.port)
  }

  private fun setupRouter(): Router {
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router.setupAllowAnyCORS()
    router.setupSockJSHandler(vertx, services, config)
    return router
  }
}