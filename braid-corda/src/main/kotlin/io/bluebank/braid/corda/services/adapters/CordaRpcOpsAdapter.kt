package io.bluebank.braid.corda.services.adapters

import io.bluebank.braid.corda.services.FlowStarter
import net.corda.core.flows.FlowLogic
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.FlowHandle

fun CordaRPCOps.asFlowStarter() : FlowStarter {
  return CordaRpcOpsAdapter(this)
}

class CordaRpcOpsAdapter(private val cordaRpcOps: CordaRPCOps) : FlowStarter {
  override fun <T> startFlowDynamic(
    logicType: Class<out FlowLogic<T>>,
    vararg args: Any?
  ): FlowHandle<T> {
    return cordaRpcOps.startFlowDynamic(logicType, *args)
  }
}