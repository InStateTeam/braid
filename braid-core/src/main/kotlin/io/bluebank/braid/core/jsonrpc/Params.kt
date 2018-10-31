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
package io.bluebank.braid.core.jsonrpc

import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.json.Json
import java.lang.reflect.Constructor
import java.lang.reflect.Parameter
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

private val log = loggerFor<Params>()

interface Params {
  companion object {
    fun build(params: Any?): Params {
      if (params == null) {
        return NullParams()
      }
      if (params is List<*>) {
        return ListParams(params as List<Any?>)
      }
      if (params is Map<*, *>) {
        @Suppress("UNCHECKED_CAST")
        return NamedParams(params as Map<String, Any?>)
      } else {
        return SingleValueParam(params)
      }
    }
  }

  val count: Int

  fun mapParams(method: KFunction<*>): List<Any?>
  fun mapParams(constructor: Constructor<*>): List<Any?>
}

abstract class AbstractParams : Params

class SingleValueParam(val param: Any) : AbstractParams() {
  override val count: Int = 1

  override fun mapParams(method: KFunction<*>): List<Any?> {
    return listOf(convert(param, method.valueParameters[0]))
  }

  override fun mapParams(constructor: Constructor<*>): List<Any?> {
    return listOf(convert(param, constructor.parameters[0]))
  }

  override fun toString(): String {
    return param.toString()
  }
}

class NamedParams(val map: Map<String, Any?>) : Params {
  override val count: Int = map.size
  override fun mapParams(method: KFunction<*>): List<Any?> {
    return method.valueParameters.map { parameter ->
      val value = map[parameter.name]
      convert(value, parameter)
    }
  }

  override fun mapParams(constructor: Constructor<*>): List<Any?> {
    return constructor.parameters.map { parameter ->
      val value = map[parameter.name]
      convert(value, parameter)
    }
  }

  override fun toString(): String {
    return map.map { "${it.key}: ${it.value}" }.joinToString(",")
  }
}

class ListParams(val params: List<Any?>) : AbstractParams() {
  override val count: Int = params.size
  override fun mapParams(method: KFunction<*>): List<Any?> {
    return method.valueParameters.zip(params).map { (parameter, value) ->
      convert(value, parameter)
    }
  }

  override fun mapParams(constructor: Constructor<*>): List<Any?> {
    return constructor.parameters.zip(params).map { (parameter, value) ->
      convert(value, parameter)
    }
  }

  override fun toString(): String {
    return params.joinToString(",") { it.toString() }
  }
}

private fun convert(value: Any?, parameter: KParameter) = convert(value, parameter.type.jvmErasure.javaObjectType)

private fun convert(value: Any?, parameter: Parameter) = convert(value, parameter.type)

private fun convert(value: Any?, clazz: Class<*>): Any? {
  return when (value) {
    null -> null
    else -> {
      try {
        Json.mapper.convertValue(value, clazz)
      } catch (err: IllegalArgumentException) {
        log.trace("failed to convert $value to $clazz")
        null
      }
    }
  }
}

class NullParams : Params {
  override val count: Int = 0

  override fun mapParams(method: KFunction<*>): List<Any?> {
    // assuming client has already checked the method parameters
    return emptyList()
  }

  override fun mapParams(constructor: Constructor<*>): List<Any?> {
    // assuming client has already checked the method parameters
    return emptyList()
  }

  override fun toString(): String {
    return ""
  }
}
