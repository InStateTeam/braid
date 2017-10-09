package io.bluebank.jsonrpc.server

import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext

fun <T : Any> RoutingContext.write(data: T) {
  val payload = Json.encode(data)
  response()
    .setStatusCode(HttpResponseStatus.OK.code())
    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
    .putHeader(HttpHeaders.CONTENT_LENGTH, payload.length.toString())
    .write(payload)
    .end()
}

fun RoutingContext.write(err: Throwable) {
  response()
    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
    .setStatusMessage(err.message)
    .end()
}

val HttpClientResponse.failed: Boolean
  get() {
    return (this.statusCode() / 100) != 2
  }