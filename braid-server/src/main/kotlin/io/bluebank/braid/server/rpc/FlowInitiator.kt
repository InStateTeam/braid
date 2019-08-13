package io.bluebank.braid.server.rpc

import net.corda.core.flows.ContractUpgradeFlow
import net.corda.core.messaging.CordaRPCOps
import java.lang.reflect.Constructor
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class FlowInitiator(val rpc: CordaRPCOps) {


    fun getInitiator(it: KClass<*>): KCallable<*> {
        val next = it.constructors.iterator().next()
        return RPCCallable(rpc,next)
    }



}