package io.bluebank.braid.server.rpc

import net.corda.core.internal.ResolveTransactionsFlow
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import java.util.Arrays.asList
import java.util.stream.Collectors.toList
import kotlin.reflect.*

class RPCCallable<T>(val rpc: CordaRPCOps, val constructor: KCallable<T>) :KCallable<T> {
    override val annotations: List<Annotation>
        get() = constructor.annotations
    override val isAbstract: Boolean
        get() = constructor.isAbstract
    override val isFinal: Boolean
        get() = constructor.isFinal
    override val isOpen: Boolean
        get() = constructor.isOpen
    override val name: String
        get() = constructor.name
    override val parameters: List<KParameter>
        get() = constructor.parameters
    override val returnType: KType
        get() = constructor.returnType
    override val typeParameters: List<KTypeParameter>
        get() = constructor.typeParameters
    override val visibility: KVisibility?
        get() = constructor.visibility



    override fun call(vararg args: Any?): T {
        println("Attempted to call with args:" + args)

      //  rpc.startFlow(::ResolveTransactionsFlow,args[0],args[0]);

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun callBy(args: Map<KParameter, Any?>): T {
        println("Attempted to call callBy!!")
        TODO("not needed for swagger call") //To change body of created functions use File | Settings | File Templates.
    }
}