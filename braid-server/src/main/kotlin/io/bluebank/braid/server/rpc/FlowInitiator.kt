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

import io.bluebank.braid.core.async.toFuture
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.synth.preferredConstructor
import io.bluebank.braid.core.synth.trampoline
import io.bluebank.braid.server.Braid
import io.vertx.core.Future
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.toObservable
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

class FlowInitiator(val rpc: CordaRPCOps) {
    private val log = loggerFor<FlowInitiator>()


    fun getInitiator(kClass: KClass<*>): KCallable<Future<Any?>> {
        val constructor = kClass.java.preferredConstructor()

        //val constructor = FooFlow::class.java.preferredConstructor()
        val fn = trampoline(constructor, HashMap()) {
            // do what you want here ...
            // e.g. call the flow directly
            // obviously, we will be invoking the flow via an interface to CordaRPCOps or ServiceHub
            // and return a Future
            log.info("About to start $kClass with args: $it")

            rpc.startFlowDynamic(kClass.java as Class<FlowLogic<*>>, *it).returnValue.toObservable().toFuture()

        }

        return fn;//RPCCallable(rpc, fn)
    }



}