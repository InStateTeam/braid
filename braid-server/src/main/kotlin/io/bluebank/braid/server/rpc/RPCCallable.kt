package io.bluebank.braid.server.rpc

import java.util.Arrays.asList
import java.util.stream.Collectors.toList
import kotlin.reflect.*

class RPCCallable<T>(val constructor: KCallable<T>) :KCallable<T> {
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


    // fails due to java.lang.ClassCastException: io.bluebank.braid.server.rpc.BodyKType cannot be cast to kotlin.reflect.jvm.internal.KTypeImpl
    private fun singleParameter(constructor: KCallable<T>): KParameter {
        return  BodyKParameter(
                asList(),
                0,
                false,
                false,
                KParameter.Kind.VALUE,
                constructor.name + "Body",
                BodyKType(constructor.parameters.stream().map { KTypeProjection(null,it.type) }.collect(toList())
                , null, false)
        )
    }


    override val returnType: KType
        get() = constructor.returnType
    override val typeParameters: List<KTypeParameter>
        get() = constructor.typeParameters
    override val visibility: KVisibility?
        get() = constructor.visibility

    init {

    }

    override fun call(vararg args: Any?): T {
        println("Attempted to call call with args:" + args)
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun callBy(args: Map<KParameter, Any?>): T {
        println("Attempted to call callBy!!")
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}