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
package io.bluebank.braid.core.http

import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext

fun <T : Any> RoutingContext.write(data: T) {
  val payload = Json.encode(data)
  response()
    .setStatusCode(200)
    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
    .putHeader(HttpHeaders.CONTENT_LENGTH, payload.length.toString())
    .write(payload)
    .end()
}

fun RoutingContext.write(err: Throwable) {
  response()
    .setStatusCode(500)
    .setStatusMessage(err.message)
    .end()
}

fun RoutingContext.write(str: String) {
  response()
    .setStatusCode(200)
    .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
    .putHeader(HttpHeaders.CONTENT_LENGTH, str.length.toString())
    .write(str)
    .end()
}

val HttpClientResponse.failed: Boolean
  get() = (this.statusCode() / 100) != 2