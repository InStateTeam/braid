package io.bluebank.jsonrpc.server

import io.vertx.core.json.Json
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
}

class SingleValueParam(val params: Any) : Params {
  override val count: Int = 1

  override fun match(method: Method): Boolean {
    return method.parameterCount == 1
  }

  override fun mapParams(method: Method): List<Any?> {
    return listOf(params)
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
}
