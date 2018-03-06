/*
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

package io.bluebank.braid.core.http

import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponseStatus
import io.swagger.annotations.ApiParam
import io.vertx.core.Future
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.javaType

fun Router.setupAllowAnyCORS() {
  route().handler {
    // allow all origins .. TODO: set this up with configuration
    val origin = it.request().getHeader("Origin")
    if (origin != null) {
      it.response().putHeader("Access-Control-Allow-Origin", origin)
      it.response().putHeader("Access-Control-Allow-Credentials", "true")
      it.response().putHeader("Access-Control-Allow-Headers", "Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With")
    }
    it.next()
  }
}

fun Router.setupOptionsMethod() {
  options().handler { it.response()
      .putHeader(HttpHeaders.ALLOW, "GET, PUT, POST, OPTIONS, CONNECT, HEAD, DELETE, CONNECT, TRACE, PATCH")
      .putHeader(HttpHeaders.CONTENT_TYPE, "text/*")
      .putHeader(HttpHeaders.CONTENT_TYPE, "application/*")
      .end()
  }
}


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


fun RoutingContext.withErrorHandler(callback: RoutingContext.() -> Unit) {
  try {
    this.callback()
  } catch (err: Throwable) {
    this.response().end(err)
  }
}

fun HttpServerResponse.end(error: Throwable) {
  val e = if (error is InvocationTargetException) error.targetException else error
  val message = if (e.message != null) e.message else "Undefined error"
  this.setStatusMessage(message)
      .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
      .end()
}

fun <T> HttpServerResponse.end(future: Future<T>) {
  future.setHandler {
    if (it.succeeded()) {
      this.end(it.result())
    } else {
      this.end(it.cause())
    }
  }
}

fun <T> HttpServerResponse.end(value: T) {
  when (value) {
    is String -> this.endWithString(value)
    is JsonArray -> this.end(value)
    is JsonObject -> this.end(value)
    else -> {
      val payload = Json.encode(value)
      this
          .putHeader(HttpHeaders.CONTENT_LENGTH, payload.length.toString())
          .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
          .end(payload)
    }
  }
}

fun HttpServerResponse.endWithString(value: String) {
  this.putHeader(HttpHeaders.CONTENT_LENGTH, value.length.toString())
      .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
      .end(value)
}

fun HttpServerResponse.end(value: JsonArray) {
  val payload = value.encode()
  this
      .putHeader(HttpHeaders.CONTENT_LENGTH, payload.length.toString())
      .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
      .end(payload)
}

fun HttpServerResponse.end(value: JsonObject) {
  val payload = value.encode()
  this
      .putHeader(HttpHeaders.CONTENT_LENGTH, payload.length.toString())
      .putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
      .end(payload)
}

private fun <R> KCallable<R>.parseArguments(context: RoutingContext): Array<Any?> {
  return this.parameters.map { it.parseParameter(context) }.toTypedArray()
}

private fun KParameter.parseParameter(context: RoutingContext): Any? {
  return this.parsePathParameter(context) ?: this.parseQueryParameter(context) ?: this.parseBodyParameter(context)
}

private fun KParameter.parseBodyParameter(context: RoutingContext): Any? {
  return context.body.let {
    if (it != null && it.length() > 0) {
      val constructType = Json.mapper.typeFactory.constructType(this.type.javaType)
      Json.mapper.readValue<Any>(it.toString(), constructType)
    } else {

    }
  }
}

private fun KParameter.parseQueryParameter(context: RoutingContext): Any? {
  return when {
    this.isSimpleType() -> {
      val parameterName = this.parameterName() ?: return null
      val queryParam = context.queryParam(parameterName).firstOrNull()
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

private fun KParameter.isSimpleType(): Boolean {
  val k = this.getType()
  return (Number::class.isSuperclassOf(k) || k == String::class || k == Boolean::class)
}

private fun KParameter.getType(): KClass<*> {
  return when (this.type.classifier) {
    is KClass<*> -> {
      this.type.classifier as KClass<*>
    }
    else -> throw RuntimeException("parameter doesn't have a class type")
  }
}

private fun KParameter.parameterName(): String? {
  return this.findAnnotation<ApiParam>()?.name ?: this.name
}





