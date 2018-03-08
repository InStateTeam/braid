package io.bluebank.braid.corda.example

import io.bluebank.braid.corda.BraidConfig
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
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
        .withAuthConstructor({ MySimpleAuthProvider() })
        .bootstrapBraid(serviceHub)
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
