package io.bluebank.braid.corda

import io.bluebank.braid.core.http.setupAllowAnyCORS
import io.bluebank.braid.core.http.withCompatibleWebsockets
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import net.corda.node.internal.Node
import net.corda.node.services.api.ServiceHubInternal

class BraidVerticle(private val services: ServiceHubInternal, private val config: BraidConfig) : AbstractVerticle() {
  companion object {
    private val log = loggerFor<BraidVerticle>()
  }
  override fun start(startFuture: Future<Void>) {
    log.info("starting with ", config)
    val router = setupRouter()
    setupWebserver(router, startFuture)
    Node.printBasicNodeInfo("Braid server started on", "http://localhost:${config.port}${config.rootPath}")
  }

  private fun setupWebserver(router: Router, startFuture: Future<Void>) {
    vertx.createHttpServer(config.httpServerOptions.withCompatibleWebsockets())
        .requestHandler(router::accept)
        .listen(config.port) {
          startFuture.completer().handle(it.mapEmpty())
        }
  }

  private fun setupRouter(): Router {
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router.setupAllowAnyCORS()
    router.setupSockJSHandler(vertx, services, config)
    return router
  }
}