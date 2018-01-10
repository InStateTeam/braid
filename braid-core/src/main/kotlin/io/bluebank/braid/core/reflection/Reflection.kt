package io.bluebank.braid.core.reflection

import io.bluebank.braid.core.annotation.ServiceDescription
import io.vertx.core.Future
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

fun <T: Any> Class<T>.serviceName() : String {
  return getDeclaredAnnotation(ServiceDescription::class.java)?.name ?: simpleName.toLowerCase()
}

fun Method.actualReturnType() : Type {
  return when {
    isAsyncResponse() -> genericReturnType.getGenericParameterType(0)
    else -> genericReturnType
  }
}

fun Method.isAsyncResponse() : Boolean {
  return returnType.isAsyncResponse()
}

fun Type.actualReturnType() : Type {
    return when(this) {
      is ParameterizedType -> {
        when {
          this.isAsynResponse() -> getGenericParameterType(0)
          else -> this
        }
      }
      else -> this
    }
}

fun Type.isAsynResponse() : Boolean {
  return when (this) {
    is Class<*> -> this.isAsynResponse()
    is ParameterizedType -> this.rawType.isAsynResponse()
    else -> false
  }
}

fun <T: Any> Class<T>.isAsyncResponse() : Boolean {
  return when (this) {
    Future::class.java, Observable::class.java -> true
    else -> false
  }
}


private fun Type.getGenericParameterType(index: Int) : Type {
  return (this as ParameterizedType).actualTypeArguments[index]
}