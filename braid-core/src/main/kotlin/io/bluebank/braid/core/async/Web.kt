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
package io.bluebank.braid.core.async

import io.netty.handler.codec.http.HttpHeaderValues
import io.vertx.core.Future
import io.vertx.core.Future.*
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

fun RoutingContext.end(text: String) {
  val length = text.length
  response().apply {
    putHeader(HttpHeaders.CONTENT_LENGTH, length.toString())
    putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
    end(text)
  }
}

fun <T : Any> RoutingContext.end(obj: T) {
  val result = Json.encode(obj)
  response().apply {
    putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
    putHeader(HttpHeaders.CONTENT_LENGTH, result.length.toString())
    end(result)
  }
}

fun RoutingContext.end(json: JsonObject) {
  val result = json.encode()
  response().apply {
    putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
    putHeader(HttpHeaders.CONTENT_LENGTH, result.length.toString())
    end(result)
  }
}

fun RoutingContext.end(json: JsonArray) {
  val result = json.encode()
  response().apply {
    putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
    putHeader(HttpHeaders.CONTENT_LENGTH, result.length.toString())
    end(result)
  }
}

fun RoutingContext.end(err: Throwable) {
  response().apply {
    statusCode = 500
    statusMessage = err.message
    end()
  }
}

fun HttpClientRequest.getBodyAsString() : Future<String> = this.getBodyBuffer().map { it.toString() }
fun HttpClientRequest.getBodyBuffer() : Future<Buffer> = this.toFuture().getBodyBuffer()

fun HttpClientRequest.toFuture() : Future<HttpClientResponse> {
  val result = future<HttpClientResponse>()
  this.handler(result::complete)
  this.exceptionHandler(result::fail)
  this.end()
  return result
}

fun Future<HttpClientResponse>.getBodyBuffer() : Future<Buffer> {
  return this.compose { response ->
    when(response.statusCode() / 100 != 2) {
       true -> failedFuture<Buffer>("failed with status ${response.statusCode()} - ${response.statusMessage()}")
      else -> {
        val result = future<Buffer>()
        response.bodyHandler(result::complete)
        result
      }
    }
  }
}
