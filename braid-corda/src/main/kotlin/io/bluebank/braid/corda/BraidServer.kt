package io.bluebank.braid.corda

import io.bluebank.braid.corda.serialisation.BraidCordaJacksonInit
import io.vertx.core.Vertx
import net.corda.core.node.ServiceHub
import net.corda.core.utilities.loggerFor
import net.corda.node.services.api.ServiceHubInternal

class BraidServer(serviceHub: ServiceHub, private val config: BraidConfig) {
  companion object {
    private val logger = loggerFor<BraidServer>()
    init {
      BraidCordaJacksonInit.init()
    }
    fun bootstrapBraid(serviceHub: ServiceHub, config: BraidConfig = BraidConfig()) : BraidServer {
      val result = BraidServer(serviceHub, config).start()
      net.corda.nodeapi.internal.addShutdownHook {
        result.shutdown()
      }
      return result
    }
  }

  private val services : ServiceHubInternal = serviceHub as ServiceHubInternal
  private lateinit var vertx: Vertx
  private var deployId : String? = null

  private fun start() : BraidServer {
    vertx = Vertx.vertx()
    vertx.deployVerticle(BraidVerticle(services, config)) {
      if (it.failed()) {
        logger.error("failed to startup braid server", it.cause())
      } else {
        deployId = it.result()
      }
    }
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

