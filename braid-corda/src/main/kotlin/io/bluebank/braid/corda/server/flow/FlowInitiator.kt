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
package io.bluebank.braid.corda.server.flow

import io.bluebank.braid.corda.server.rpc.RPCCallable
import io.bluebank.braid.corda.services.FlowStarterAdapter
import io.bluebank.braid.core.async.toFuture
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.synth.preferredConstructor
import io.bluebank.braid.core.synth.trampoline
import io.vertx.core.Future
import net.corda.core.flows.FlowLogic
import net.corda.core.toObservable
import net.corda.core.utilities.ProgressTracker
import kotlin.reflect.KCallable
import kotlin.reflect.KClass

class FlowInitiator(private val flowStarter: FlowStarterAdapter) {
  private val log = loggerFor<FlowInitiator>()

  fun getInitiator(kClass: KClass<*>): KCallable<Future<Any?>> {
    val constructor = kClass.java.preferredConstructor()

    //val constructor = FooFlow::class.java.preferredConstructor()
    val fn = trampoline(constructor, createBoundParameterTypes()) {
      // do what you want here ...
      // e.g. call the flow directly
      // obviously, we will be invoking the flow via an interface to CordaRPCOps or ServiceHub
      // and return a Future
      val excludeProgressTracker = it.toMutableList()
      //todo might have other classes that aren't in startFlowDynamic
      excludeProgressTracker.removeIf { l -> l is ProgressTracker }
      log.info("About to start $kClass with args: ${listOf(it)}")

      @Suppress("UNCHECKED_CAST")
      flowStarter.startFlowDynamic(
        kClass.java as Class<FlowLogic<*>>,
        *excludeProgressTracker.toTypedArray()
      ).returnValue.toObservable().toFuture()
    }

    return RPCCallable(kClass, fn)
  }

  private fun createBoundParameterTypes(): Map<Class<*>, Any> {
    return mapOf<Class<*>, Any>(ProgressTracker::class.java to ProgressTracker())
  }
}