package io.bluebank.braid.server.rpc

import kotlin.reflect.*

class RPCCallable<T>(val callable: KCallable<T>) :KCallable<T> {
    override val annotations: List<Annotation>
        get() = callable.annotations
    override val isAbstract: Boolean
        get() = callable.isAbstract
    override val isFinal: Boolean
        get() = callable.isFinal
    override val isOpen: Boolean
        get() = callable.isOpen
    override val name: String
        get() = callable.name
    override val parameters: List<KParameter>
        get() = callable.parameters
    override val returnType: KType
        get() = callable.returnType
    override val typeParameters: List<KTypeParameter>
        get() = callable.typeParameters
    override val visibility: KVisibility?
        get() = callable.visibility

    init {

    }

    override fun call(vararg args: Any?): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun callBy(args: Map<KParameter, Any?>): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}