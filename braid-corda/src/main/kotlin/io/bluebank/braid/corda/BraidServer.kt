package io.bluebank.braid.corda

import io.bluebank.braid.corda.serialisation.BraidCordaJacksonInit
import io.vertx.core.Vertx
import net.corda.core.node.ServiceHub
import net.corda.core.utilities.loggerFor
import net.corda.node.services.api.ServiceHubInternal
import net.corda.nodeapi.internal.addShutdownHook

class BraidServer(serviceHub: ServiceHub, private val config: BraidConfig) {
  companion object {
    private val log = loggerFor<BraidServer>()
    init {
      BraidCordaJacksonInit.init()
    }
    fun bootstrapBraid(serviceHub: ServiceHub, config: BraidConfig = BraidConfig()) =
        BraidServer(serviceHub, config).start()
  }

  private val services : ServiceHubInternal = serviceHub as ServiceHubInternal
  private lateinit var vertx: Vertx
  private var deployId : String? = null

  private fun start() : BraidServer {
    vertx = Vertx.vertx()
    vertx.deployVerticle(BraidVerticle(services, config)) {
      if (it.failed()) {
        log.error("failed to start braid server on ${config.port}")
      } else {
        log.info("braid server started successfully on ${config.port}")
        deployId = it.result()
      }
    }
    addShutdownHook(this::shutdown)
    return this
  }

  fun shutdown() {
    if (deployId != null) {
      vertx.undeploy(deployId) // async
      deployId = null
      vertx.close() // async
    }
  }
}

