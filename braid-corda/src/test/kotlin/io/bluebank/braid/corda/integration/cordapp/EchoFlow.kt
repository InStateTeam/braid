package io.bluebank.braid.corda.integration.cordapp

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic

class EchoFlow(private val text: String) : FlowLogic<String>() {
  @Suspendable
  override fun call(): String {
    return "Echo: $text"
  }
}
