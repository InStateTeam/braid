package io.bluebank.hermes.core.http

import io.vertx.ext.web.Router

fun Router.setupAllowAnyCORS() {
  route().handler {
    // allow all origins .. TODO: set this up with configuration
    val origin = it.request().getHeader("Origin")
    if (origin != null) {
      it.response().putHeader("Access-Control-Allow-Origin", origin)
      it.response().putHeader("Access-Control-Allow-Credentials", "true")
    }
    it.next()
  }
}
