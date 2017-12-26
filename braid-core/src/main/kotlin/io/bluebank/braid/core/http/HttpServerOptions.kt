package io.bluebank.braid.core.http

import io.vertx.core.http.HttpServerOptions

fun HttpServerOptions.withCompatibleWebsockets(): HttpServerOptions {
  this.websocketSubProtocols = "undefined"
  return this
}