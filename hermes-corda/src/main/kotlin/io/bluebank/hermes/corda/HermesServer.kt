package io.bluebank.hermes.corda

import io.bluebank.hermes.corda.serialisation.HermesCordaJacksonInit
import io.vertx.core.Vertx
import net.corda.core.node.ServiceHub
import net.corda.core.utilities.loggerFor
import net.corda.node.services.api.ServiceHubInternal

class HermesServer(serviceHub: ServiceHub, private val config: HermesConfig) {
  companion object {
    private val logger = loggerFor<HermesServer>()
    init {
      HermesCordaJacksonInit.init()
    }
    fun bootstrapHermes(serviceHub: ServiceHub, config: HermesConfig = HermesConfig()) =
        HermesServer(serviceHub, config).start()
  }

  private val services : ServiceHubInternal = serviceHub as ServiceHubInternal
  private lateinit var vertx: Vertx
  private var deployId : String? = null

  private fun start() : HermesServer {
    vertx = Vertx.vertx()
    vertx.deployVerticle(HermesVerticle(services, config)) {
      if (it.failed()) {
        logger.error("failed to startup hermes server", it.cause())
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

