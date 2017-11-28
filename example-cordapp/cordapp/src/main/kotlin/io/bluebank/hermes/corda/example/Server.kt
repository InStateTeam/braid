package io.bluebank.hermes.corda.example

import io.bluebank.hermes.corda.HermesConfig
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import net.corda.finance.flows.CashIssueFlow
import net.corda.node.services.api.ServiceHubInternal

@CordaService
class Server(private val serviceHub: ServiceHub) : SingletonSerializeAsToken() {
  companion object {
    private val log = loggerFor<Server>()
  }

  init {
    val config = HermesConfig.fromResource(configFileName)
    if (config == null) {
      log.warn("config $configFileName not found")
    } else {
      bootstrap(config)
    }
  }

  private fun bootstrap(config: HermesConfig) {
    val shiroConfig = json {
      obj {
        put("properties_path", "classpath:shiro.properties")
      }
    }
    config
        .withFlow(EchoFlow::class)
        .withFlow("issueCash", CashIssueFlow::class)
        .withService("myService", MyService(serviceHub as ServiceHubInternal))
        .withAuthConstructor { ShiroAuth.create(it, ShiroAuthOptions().setConfig(shiroConfig)) }
        .bootstrapHermes(serviceHub)
  }

  private val configFileName: String
    get() {
      val name = serviceHub.myInfo.legalIdentities.first().name.organisation
      return "hermes-$name.json"
    }
}