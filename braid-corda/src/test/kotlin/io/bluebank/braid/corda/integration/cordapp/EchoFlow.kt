package io.bluebank.braid.corda.integration.cordapp

import net.corda.core.flows.FlowLogic

class EchoFlow(private val text: String) : FlowLogic<String>() {
  override fun call(): String {
    return "Echo: $text"
  }
}