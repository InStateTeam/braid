package io.bluebank.jsonrpc.server

import io.vertx.core.AsyncResult
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Vertx
import io.vertx.ext.auth.AuthProvider

/**
 * configuration options for JsonRPCServer
 */
class JsonRPCServerBuilder {
  internal var vertx: Vertx? = null
  internal var rootPath: String = "/api/"
  internal var port: Int = 8080
  internal var services: MutableList<Any> = mutableListOf()
  internal var authProvider: AuthProvider? = null

  companion object {
    /**
     * main entry point to setup a builder
     * following this, call the fluent api to setup options of the builder
     * and finally call [build]
     */
    @JvmStatic
    fun createServerBuilder(): JsonRPCServerBuilder {
      return JsonRPCServerBuilder()
    }
  }

  fun withVertx(vertx: Vertx): JsonRPCServerBuilder {
    this.vertx = vertx
    return this
  }

  fun withRootPath(rootPath: String): JsonRPCServerBuilder {
    if (!rootPath.endsWith('/')) {
      throw IllegalArgumentException("path must end with /")
    }
    this.rootPath = rootPath
    return this
  }

  fun withPort(port: Int): JsonRPCServerBuilder {
    this.port = port
    return this
  }

  fun withServices(services: Collection<Any>): JsonRPCServerBuilder {
    this.services.clear()
    this.services.addAll(services)
    return this
  }

  fun withService(service: Any): JsonRPCServerBuilder {
    this.services.add(service)
    return this
  }

  fun withAuthProvider(authProvider: AuthProvider?): JsonRPCServerBuilder {
    this.authProvider = authProvider
    return this
  }

  fun build() : JsonRPCServer {
    return JsonRPCServer.createJsonRpcServer(this)
  }
}


class JsonRPCServer private constructor(private val builder: JsonRPCServerBuilder) {
  companion object {
    init {
      JacksonKotlinInit.init()
    }

    internal fun createJsonRpcServer(builder: JsonRPCServerBuilder): JsonRPCServer {
      return JsonRPCServer(builder)
    }
  }

  var deploymentId: String? = null

  init {
    if (builder.vertx == null) {
      builder.vertx = Vertx.vertx()
    }
  }

  /**
   * Start the server
   * The startup is asynchronous.
   */
  fun start() {
    start { }
  }

  /**
   * Start the server and callback on [callback] function
   * Clients should check the status of the [AsyncResult]
   */
  fun start(callback: (AsyncResult<Void>) -> Unit) {
    if (deploymentId == null) {
      with(builder) {
        vertx!!.deployVerticle(JsonRPCVerticle(rootPath, services, port, authProvider)) {
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
  }

  /**
   * Asynchronously stop the sever
   */
  fun stop() {
    stop {}
  }

  /**
   * Asynchronously stop the server
   * Calls back on [callback] function with the result of the operation
   * Clients should check the [AsyncResult] state
   */
  fun stop(callback: (AsyncResult<Void>) -> Unit) {
    if (deploymentId != null) {
      builder.vertx!!.undeploy(deploymentId, callback)
      deploymentId = null
    }
  }
}

