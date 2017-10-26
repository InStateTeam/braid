package io.bluebank.jsonrpc.server

import io.vertx.core.AsyncResult
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Vertx
import io.vertx.ext.auth.AuthProvider

class JsonRPCServer(val vertx: Vertx,
                    val rootPath: String = "/api/",
                    val port: Int = 8080,
                    val services: List<Any> = listOf(),
                    val authProvider: AuthProvider? = null) {
  companion object {
    init {
      JacksonKotlinInit.init()
    }
  }

  var deploymentId: String? = null

  init {
    if (!rootPath.endsWith("/")) {
      throw RuntimeException("path must end with '/': $rootPath")
    }
  }

  fun start() {
    start { }
  }

  fun start(callback: (AsyncResult<Void>) -> Unit) {
    if (deploymentId == null) {
      vertx.deployVerticle(JsonRPCVerticle(rootPath, services, port, authProvider)) {
        if (it.failed()) {
          println("failed to deploy: ${it.cause().message}")
          callback(failedFuture(it.cause()))
        } else {
          deploymentId = it.result()
          println("server mounted on http://localhost:$port$rootPath")
          println("sockjs JsonRPC server mounted on http://localhost:$port${rootPath}jsonrpc/")
          println("editor mounted on http://localhost:$port")
          callback(succeededFuture())
        }
      }
    }
  }

  fun stop() {
    stop {}
  }

  fun stop(callback: (AsyncResult<Void>) -> Unit) {
    if (deploymentId != null) {
      vertx.undeploy(deploymentId, callback)
      deploymentId = null
    }
  }
}

