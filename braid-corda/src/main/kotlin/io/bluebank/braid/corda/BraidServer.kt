/*
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

import io.bluebank.braid.corda.serialisation.BraidCordaJacksonInit
import io.vertx.core.Vertx
import net.corda.core.node.AppServiceHub
import net.corda.core.utilities.loggerFor
import net.corda.nodeapi.internal.addShutdownHook
import java.util.concurrent.ConcurrentHashMap

class BraidServer(private val services: AppServiceHub, private val config: BraidConfig) {
  companion object {
    private val log = loggerFor<BraidServer>()
    private val servers = ConcurrentHashMap<Int, BraidServer>()

    init {
      BraidCordaJacksonInit.init()
    }

    fun bootstrapBraid(serviceHub: AppServiceHub, config: BraidConfig = BraidConfig()) : BraidServer {
      val result = servers.computeIfAbsent(config.port) {
        log.info("starting up braid server for ${serviceHub.myInfo.legalIdentities.first().name.organisation} on port ${config.port}")
        BraidServer(serviceHub, config).start()
      }
      serviceHub.registerUnloadHandler {
        result.shutdown()
      }
      return result
    }
  }

  lateinit var vertx: Vertx
  private set

  private var deployId : String? = null
  private fun start() : BraidServer {
    vertx = Vertx.vertx()
    vertx.deployVerticle(BraidVerticle(services, config)) {
      if (it.failed()) {
        log.error("failed to start braid server on ${config.port}", it.cause())
      } else {
        log.info("Braid server started successfully on ${config.port}")
        deployId = it.result()
      }
    }
    addShutdownHook(this::shutdown)
    return this
  }

  fun shutdown() {
    if (deployId != null) {
      log.info("shutting down braid server on port: ${config.port}")
      vertx.undeploy(deployId) {
        deployId = null
        vertx.close()
      }
    }
  }
}

