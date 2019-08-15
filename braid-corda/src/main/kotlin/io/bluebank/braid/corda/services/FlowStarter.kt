package io.bluebank.braid.corda.services

import net.corda.core.flows.FlowLogic
import net.corda.core.messaging.FlowHandle

interface FlowStarter {
  fun <T> startFlowDynamic(logicType: Class<out FlowLogic<T>>, vararg args: Any?): FlowHandle<T>
}

