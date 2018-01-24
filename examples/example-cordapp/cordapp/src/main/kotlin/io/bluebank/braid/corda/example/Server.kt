package io.bluebank.braid.corda.example

import io.bluebank.braid.corda.BraidConfig
import io.vertx.core.Vertx
import io.vertx.ext.auth.AuthProvider
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

  init {
    BraidConfig.fromResource(configFileName)?.bootstrap()
  }

  private fun BraidConfig.bootstrap() {
    this.withFlow(EchoFlow::class)
        .withFlow("issueCash", CashIssueFlow::class)
        .withService("myService", MyService(serviceHub as ServiceHubInternal))
        .withAuthConstructor({ createAuthProvider(it) })
        .bootstrapBraid(serviceHub)
  }

  private fun createAuthProvider(it: Vertx): AuthProvider {
    // this can be any auth provider
    // important for integration into enterprise Auth* services
    // for now, we just use a Shiro in-memory provider
    val shiroConfig = json {
      obj {
        put("properties_path", "classpath:shiro.properties")
      }
    }
    return ShiroAuth.create(it, ShiroAuthOptions().setConfig(shiroConfig))
  }

  /**
   * config file name based on the node legal identity
   */
  private val configFileName: String
    get() {
      val name = serviceHub.myInfo.legalIdentities.first().name.organisation
      return "braid-$name.json"
    }
}