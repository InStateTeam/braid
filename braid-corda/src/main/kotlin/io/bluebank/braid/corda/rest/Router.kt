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
package io.bluebank.braid.corda.rest

import io.bluebank.braid.core.http.end
import io.bluebank.braid.core.http.parseQueryParams
import io.bluebank.braid.core.http.withErrorHandler
import io.bluebank.braid.core.jsonrpc.Converter
import io.netty.buffer.ByteBuf
import io.swagger.annotations.ApiParam
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import java.nio.ByteBuffer
import javax.ws.rs.HeaderParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.HttpHeaders
import kotlin.reflect.*
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.javaType

fun <R> Route.bind(fn: KCallable<R>) {
  fn.validateParameters()
  this.handler { rc ->
    rc.withErrorHandler {
      val args = fn.parseArguments(rc)
      val result = fn.call(*args)
      rc.response().end(result)
    }
  }
}

@JvmName("bindOnFuture")
fun <R> Route.bind(fn: KCallable<Future<R>>) {
  fn.validateParameters()
  this.handler { rc ->
    rc.withErrorHandler {
      val args = fn.parseArguments(rc)
      rc.response().end(fn.call(*args))
    }
  }
}

private fun <R> KCallable<R>.parseArguments(context: RoutingContext): Array<Any?> {
  return this.parameters.map { it.parseParameter(context) }.toTypedArray()
}

private fun KParameter.parseBodyParameter(context: RoutingContext): Any? {
  return context.body.let { body ->
    val type = this.getType()
    when {
      type.isSubclassOf(Buffer::class) -> {
        body
      }
      type.isSubclassOf(ByteArray::class) -> {
        body.bytes
      }
      type.isSubclassOf(ByteBuf::class) -> {
        body.byteBuf
      }
      type.isSubclassOf(ByteBuffer::class) -> {
        ByteBuffer.wrap(body.bytes)
      }
      type == String::class -> {
        body.toString()
      }
      else -> {
        if (body != null && body.length() > 0) {
          val constructType = Json.mapper.typeFactory.constructType(this.type.javaType)
          Json.mapper.readValue<Any>(body.toString(), constructType)
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
  return this.parsePathParameter(context)
    ?: this.parseQueryParameter(context)
    ?: this.parseContextParameter(context)
    ?: this.parseHeaderParameter(context)
    ?: this.parseBodyParameter(context)
}

private fun KParameter.parseContextParameter(context: RoutingContext): Any? {
  this.findAnnotation<Context>() ?: return null
  // we always map and pass HttpHeaders
  assert(this.getType().isSubclassOf(HttpHeaders::class)) { error("expected parameter to be of type ${HttpHeaders::class.qualifiedName}")}
  return HttpHeadersImpl(context)
}

private fun KParameter.parseHeaderParameter(context: RoutingContext): Any? {
  val annotation = this.findAnnotation<HeaderParam>() ?: return null
  val headerName = annotation.value
  val values = context.request().headers().getAll(headerName)
  return when (this.type.classifier) {
    List::class -> this.parseHeaderValuesAsList(values)
    Set::class -> this.parseHeaderValuesAsSet(values)
    else -> this.type.parseHeaderValue(values.first())
  }
}

private fun KParameter.parseHeaderValuesAsList(values: List<String>) : List<*> {
  return values.map { this.type.arguments.first().parseHeaderValue(it) }
}

private fun KParameter.parseHeaderValuesAsSet(values: List<String>) : Set<*> {
  return parseHeaderValuesAsList(values).toSet()
}

private fun KTypeProjection.parseHeaderValue(value: String): Any {
  val type = this.type
  return when (type) {
    null -> value
    else -> type.parseHeaderValue(value)!!
  }
}

private fun KType.parseHeaderValue(value: String?): Any? {
  return Converter.convert(value, this)
}

private fun KParameter.parseQueryParameter(context: RoutingContext): Any? {
  return when {
    this.isSimpleType() -> {
      val parameterName = this.parameterName() ?: return null
      val queryParam = context.request().query()?.parseQueryParams()?.get(parameterName)
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

private fun <R> KCallable<R>.validateParameters() {
  return this.parameters.forEach { it.validateParameter() }
}

private fun KParameter.validateParameter() {
  this.validateContextAnnotation()
  this.validateHeaderParamAnnotation()
}

private fun KParameter.validateHeaderParamAnnotation() {
  this.findAnnotation<HeaderParam>() ?: return
  val type = this.getType()
  when (type) {
    String::class -> {
      assert(this.isOptional) { "parameter ${this.name} should be nullable" }
    }
    List::class, Set::class -> {
//      val args = this.type.arguments
//      val argType = args.first().type!!.classifier as KClass<*>
//      assert(argType == String::class) { "the collection type for parameter ${this.name} should be String" }
    }
    else -> {
      error ("parameter ${this.name} is not a String?, List<String>, or Set<String>")
    }
  }
}

private fun KParameter.validateContextAnnotation() {
  this.findAnnotation<Context>() ?: return
  assert(this.getType().isSubclassOf(HttpHeaders::class)) {
    "braid only supports @${HttpHeaders::class.simpleName} parameters annotated with @${Context::class.simpleName} but parameter ${this.name} is of type ${this.getType()}"
  }
}