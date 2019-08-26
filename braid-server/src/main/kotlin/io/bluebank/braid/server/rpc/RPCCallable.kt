/**
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bluebank.braid.server.rpc

import net.corda.core.messaging.CordaRPCOps
import kotlin.reflect.*

class RPCCallable<T>(val rpc: CordaRPCOps, val constructor: KCallable<T>) :KCallable<T> {
//    override val isSuspend: Boolean
//        get() = constructor.isSuspend
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
        println("Attempted to call callBy!!" + args)
        TODO("not needed for swagger call") //To change body of created functions use File | Settings | File Templates.
    }
}