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
package io.bluebank.braid.corda.services.adapters

import com.google.common.primitives.Primitives
import io.bluebank.braid.corda.rest.docs.javaTypeIncludingSynthetics
import io.bluebank.braid.corda.services.FlowStarter
import net.corda.core.flows.FlowLogic
import net.corda.core.internal.uncheckedCast
import net.corda.core.messaging.FlowHandle
import net.corda.core.node.AppServiceHub
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.util.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.jvm.javaConstructor

fun AppServiceHub.asFlowStarter(): FlowStarter {
  return AppServiceHubAdapter(this)
}

class AppServiceHubAdapter(private val serviceHub: AppServiceHub) : FlowStarter {
  override fun <T> startFlowDynamic(
    logicType: Class<out FlowLogic<T>>,
    vararg args: Any?
  ): FlowHandle<T> {
    val argTypes = args.map { it?.javaClass }
    val constructor: KFunction<FlowLogic<T>> =
      uncheckedCast(findConstructor(logicType, argTypes))
    val argsMap =
      args.zip(constructor.parameters).map { Pair(it.second.name!!, it.first) }.toMap()
    val params =
      buildParams(constructor, argsMap) ?: error("could not find matching constructor")
    val flowLogic = constructor.callBy(params)
    return serviceHub.startFlow(flowLogic)
  }

  private fun findConstructor(
    flowClass: Class<out FlowLogic<*>>,
    argTypes: List<Class<Any>?>
  ): KFunction<FlowLogic<*>> {
    return flowClass.kotlin.constructors.single { ctor ->
      // Get the types of the arguments, always boxed (as that's what we get in the invocation).
      val ctorTypes = ctor.javaConstructor!!.parameterTypes.map { Primitives.wrap(it) }
      if (argTypes.size != ctorTypes.size)
        return@single false
      for ((argType, ctorType) in argTypes.zip(ctorTypes)) {
        if (argType == null) continue   // Try and find a match based on the other arguments.
        if (!ctorType.isAssignableFrom(argType)) return@single false
      }
      true
    }

  }

  private fun buildParams(
    constructor: KFunction<FlowLogic<*>>,
    args: Map<String, Any?>
  ): HashMap<KParameter, Any?>? {
    val params = hashMapOf<KParameter, Any?>()
    val usedKeys = hashSetOf<String>()
    for (parameter in constructor.parameters) {
      if (!tryBuildParam(args, parameter, params)) {
        return null
      } else {
        usedKeys += parameter.name!!
      }
    }
    if ((args.keys - usedKeys).isNotEmpty()) {
      // Not all args were used
      return null
    }
    return params
  }

  private fun tryBuildParam(
    args: Map<String, Any?>,
    parameter: KParameter,
    params: HashMap<KParameter, Any?>
  ): Boolean {
    val containsKey = parameter.name in args
    // OK to be missing if optional
    return (parameter.isOptional && !containsKey) || (containsKey && paramCanBeBuilt(
      args,
      parameter,
      params
    ))
  }

  private fun paramCanBeBuilt(
    args: Map<String, Any?>,
    parameter: KParameter,
    params: HashMap<KParameter, Any?>
  ): Boolean {
    val value = args[parameter.name]
    params[parameter] = value
    return (value is Any && parameterAssignableFrom(
      parameter.type.javaTypeIncludingSynthetics(),
      value
    )) || parameter.type.isMarkedNullable
  }

  private fun parameterAssignableFrom(type: Type, value: Any): Boolean {
    return if (type is Class<*>) {
      if (type.isPrimitive) {
        Primitives.unwrap(value.javaClass) == type
      } else {
        type.isAssignableFrom(value.javaClass)
      }
    } else if (type is ParameterizedType) {
      parameterAssignableFrom(type.rawType, value)
    } else if (type is TypeVariable<*>) {
      type.bounds.all { parameterAssignableFrom(it, value) }
    } else {
      false
    }
  }
}