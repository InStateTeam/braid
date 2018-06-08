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
package io.bluebank.braid.corda.restafarian

import io.bluebank.braid.core.http.end
import io.bluebank.braid.core.http.parseQueryParams
import io.bluebank.braid.core.http.withErrorHandler
import io.netty.buffer.ByteBuf
import io.swagger.annotations.ApiParam
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import java.nio.ByteBuffer
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.javaType

fun <R> Route.bind(fn: KCallable<R>) {
  this.handler {
    it.withErrorHandler {
      val args = fn.parseArguments(it)
      val result = fn.call(*args)
      it.response().end(result)
    }
  }
}

@JvmName("bindOnFuture")
fun <R> Route.bind(fn: KCallable<Future<R>>) {
  this.handler {
    it.withErrorHandler {
      val args = fn.parseArguments(it)
      it.response().end(fn.call(*args))
    }
  }
}

private fun <R> KCallable<R>.parseArguments(context: RoutingContext): Array<Any?> {
  return this.parameters.map { it.parseParameter(context) }.toTypedArray()
}


private fun KParameter.parseBodyParameter(context: RoutingContext): Any? {
  return context.body.let {
    when (this.getType()) {
      Buffer::class -> {
        it
      }
      ByteArray::class -> {
        it.bytes
      }
      ByteBuf::class -> {
        it.byteBuf
      }
      ByteBuffer::class -> {
        ByteBuffer.wrap(it.bytes)
      }
      else -> {
        if (it != null && it.length() > 0) {
          val constructType = Json.mapper.typeFactory.constructType(this.type.javaType)
          Json.mapper.readValue<Any>(it.toString(), constructType)
        } else {
          null
        }
      }
    }
  }
}

private fun KParameter.parseSimpleType(paramString: String): Any {
  val k = this.getType()
  return when (k) {
    Int::class -> paramString.toInt()
    Double::class -> paramString.toDouble()
    Float::class -> paramString.toFloat()
    Boolean::class -> paramString.toBoolean()
    Short::class -> paramString.toShort()
    Long::class -> paramString.toLong()
    Byte::class -> paramString.toByte()
    String::class -> paramString
    else -> throw RuntimeException("don't know how to simple-parse $k")
  }
}

private fun KParameter.getType(): KClass<*> {
  return when (this.type.classifier) {
    is KClass<*> -> {
      this.type.classifier as KClass<*>
    }
    else -> throw RuntimeException("parameter doesn't have a class type")
  }
}

private fun KParameter.parseParameter(context: RoutingContext): Any? {
  return this.parsePathParameter(context) ?: this.parseQueryParameter(context) ?: this.parseBodyParameter(context)
}

private fun KParameter.parseQueryParameter(context: RoutingContext): Any? {
  return when {
    this.isSimpleType() -> {
      val parameterName = this.parameterName() ?: return null
      val queryParam = context.request().query().parseQueryParams().get(parameterName)
      // TODO: handle arrays
      if (queryParam != null) {
        this.parseSimpleType(queryParam)
      } else {
        null
      }
    }
    else -> {
      null
    }
  }
}

private fun KParameter.parsePathParameter(context: RoutingContext): Any? {
  return when {
    this.isSimpleType() -> {
      val parameterName = this.parameterName() ?: return null
      val paramString = context.pathParam(parameterName)
      if (paramString == null) {
        null
      } else {
        this.parseSimpleType(paramString)
      }
    }
    else -> {
      null
    }
  }
}

private fun KParameter.isSimpleType(): Boolean {
  val k = this.getType()
  return (Number::class.isSuperclassOf(k) || k == String::class || k == Boolean::class)
}

private fun KParameter.parameterName(): String? {
  return this.findAnnotation<ApiParam>()?.name ?: this.name
}
