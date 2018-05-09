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

import io.vertx.core.json.Json
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Parameter

interface Params {
  companion object {
    fun build(params: Any?) : Params {
      if (params == null) {
        return NullParams()
      }
      if (params is List<*>) {
        return ListParams(params as List<Any?>)
      }
      if (params is Map<*, *>) {
        @Suppress("UNCHECKED_CAST")
        return NamedParams(params as Map<String, Any?>)
      }
      else {
        return SingleValueParam(params)
      }
    }
  }
  val count: Int

  fun match(method: Method): Boolean
  fun mapParams(method: Method): List<Any?>
  fun mapParams(constructor: Constructor<*>) : List<Any?>
}

class SingleValueParam(val param: Any) : Params {
  override val count: Int = 1

  override fun match(method: Method): Boolean {
    return method.parameterCount == 1
  }

  override fun mapParams(method: Method): List<Any?> {
    return listOf(convert(param, method.parameters[0]))
  }

  override fun mapParams(constructor: Constructor<*>): List<Any?> {
    return listOf(convert(param, constructor.parameters[0]))
  }
}

class NamedParams(val map: Map<String, Any?>) : Params {
  override val count: Int = map.size
  override fun match(method: Method): Boolean {
    return method.parameters.all { map.containsKey(it.name) }
  }
  override fun mapParams(method: Method): List<Any?> {
    return method.parameters.map { parameter ->
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
}

class ListParams(val params: List<Any?>) : Params {
  override val count: Int = params.size

  override fun match(method: Method): Boolean {
    return (count == method.parameterCount)
  }

  override fun mapParams(method: Method): List<Any?> {
    return method.parameters.zip(params).map { (parameter, value) ->
      convert(value, parameter)
    }
  }

  override fun mapParams(constructor: Constructor<*>): List<Any?> {
    return constructor.parameters.zip(params).map { (parameter, value) ->
      convert(value, parameter)
    }
  }
}

private fun convert(value: Any?, parameter: Parameter): Any? {
  return when (value) {
    null -> null
    else -> Json.mapper.convertValue(value, parameter.type)
  }
}

class NullParams : Params {
  override val count: Int = 0

  override fun match(method: Method): Boolean {
    return method.parameterCount == 0
  }

  override fun mapParams(method: Method): List<Any?> {
    // assuming client has already checked the method parameters
    return emptyList()
  }
  override fun mapParams(constructor: Constructor<*>): List<Any?> {
    // assuming client has already checked the method parameters
    return emptyList()
  }
}
