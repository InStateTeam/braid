package io.bluebank.braid.server.rpc

import net.corda.core.flows.ContractUpgradeFlow
import net.corda.core.messaging.CordaRPCOps
import java.lang.reflect.Constructor
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class FlowInitiator(rpc: CordaRPCOps) {

    fun getInitiator(): KFunction<Any> {
        val classobj=ContractUpgradeFlow.Initiate::class
        val next = classobj.constructors.iterator().next()
        return next
    }


    fun getInitiator(it: KClass<*>): KCallable<*> {
        val next = it.constructors.iterator().next()
        return RPCCallable(next)
    }

    fun getInitiatorFunction(it: KClass<*>): KCallable<*> {
        val next = it.constructors.iterator().next()
        return RPCCallable(next)
    }


}