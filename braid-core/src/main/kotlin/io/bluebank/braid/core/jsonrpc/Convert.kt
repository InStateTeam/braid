package io.bluebank.braid.core.jsonrpc

import io.vertx.core.json.Json
import java.lang.reflect.Parameter
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

object Converter {
  fun convert(value: Any?, parameter: KParameter) = convert(value, parameter.type.jvmErasure.javaObjectType)

  fun convert(value: Any?, parameter: Parameter) = convert(value, parameter.type)

  fun convert(value: Any?, type: KType) = convert(value, type.jvmErasure.javaObjectType)

  fun convert(value: Any?, clazz: Class<*>): Any? {
    return when (value) {
      null -> null
      else -> {
        Json.mapper.convertValue(value, clazz)
      }
    }
  }
}
