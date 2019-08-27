/**
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bluebank.braid.corda

import io.bluebank.braid.corda.rest.RestMounter
import io.bluebank.braid.corda.router.Routers
import io.bluebank.braid.core.http.setupAllowAnyCORS
import io.bluebank.braid.core.http.setupOptionsMethod
import io.bluebank.braid.core.http.withCompatibleWebsockets
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import net.corda.core.node.AppServiceHub
import net.corda.node.internal.Node
import java.net.URL

class BraidVerticle(
  private val services: AppServiceHub?,
  private val config: BraidConfig
) : AbstractVerticle() {

  companion object {
    private val log = loggerFor<BraidVerticle>()
  }

  override fun start(startFuture: Future<Void>) {
    log.info("starting with ", config)
    val router = setupRouter()
    setupWebserver(router, startFuture)
    log.info(
      "Braid server started on",
      "${config.protocol}://localhost:${config.port}${config.rootPath}"
    )
  }

  private fun setupWebserver(router: Router, startFuture: Future<Void>) {
    vertx.createHttpServer(config.httpServerOptions.withCompatibleWebsockets())
      .requestHandler(router)
      .listen(config.port) {
        if (it.succeeded()) {
          log.info("Braid service mounted on ${config.protocol}://localhost:${config.port}${config.rootPath}")
        } else {
          log.error("failed to start server: ${it.cause().message}")
        }
        startFuture.handle(it.mapEmpty())
      }
  }

  private fun setupRouter(): Router {
    val router = Routers.create(vertx, config.port)
    router.route().handler(LoggerHandler.create(LoggerFormat.SHORT))
    router.route().handler(BodyHandler.create())
    router.setupAllowAnyCORS()
    router.setupOptionsMethod()
    services?.let {
      router.setupSockJSHandler(vertx, services, config)
    }
    config.restConfig?.let { restConfig ->
      val host = URL(restConfig.hostAndPortUri).host
      val updatedHostAndPort = "${config.protocol}://$host:${config.port}"
      val moddedConfig = restConfig.withHostAndPortUri(updatedHostAndPort)
        .withAuth(config.authConstructor?.invoke(vertx))

      RestMounter.mount(moddedConfig, router, vertx)
    }
    return router
  }
}