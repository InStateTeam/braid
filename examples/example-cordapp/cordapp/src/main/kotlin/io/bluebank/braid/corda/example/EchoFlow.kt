package io.bluebank.braid.corda.example

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC

@InitiatingFlow
@StartableByRPC
class EchoFlow(private val text: String) : FlowLogic<String>() {
    @Suspendable
    override fun call() : String = text
}


