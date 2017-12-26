package io.bluebank.braid.server

import io.bluebank.braid.core.http.HttpServerConfig
import io.vertx.core.AsyncResult
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
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
  internal var httpServerOptions: HttpServerOptions = HttpServerConfig.defaultServerOptions()
  companion object {
    /**
     * main entry point to setup a builder
     * following this, call the fluent api to setup options of the builder
     * and finally call [build]
     */
    @JvmStatic
    fun createServerBuilder() = JsonRPCServerBuilder()
  }

  /**
   * set the vertx container to run this server
   * default: creates a dedicated server
   */
  fun withVertx(vertx: Vertx): JsonRPCServerBuilder {
    this.vertx = vertx
    return this
  }

  /**
   * root http path that the services for this server are mounted
   * default is: /api/
   * note: path must end in /
   */
  fun withRootPath(rootPath: String): JsonRPCServerBuilder {
    if (!rootPath.endsWith('/')) {
      throw IllegalArgumentException("path must end with /")
    }
    this.rootPath = rootPath
    return this
  }

  /**
   * port that this server will bind to
   * default: 8080
   */
  fun withPort(port: Int): JsonRPCServerBuilder {
    this.port = port
    return this
  }

  /**
   * the set of services that will be exposed by this server
   * default: empty list
   */
  fun withServices(services: Collection<Any>): JsonRPCServerBuilder {
    this.services.clear()
    this.services.addAll(services)
    return this
  }

  /**
   * adds [service] to the list of services that will be exposed on this server
   */
  fun withService(service: Any): JsonRPCServerBuilder {
    this.services.add(service)
    return this
  }

  /**
   * sets the [AuthProvider] for authentication / authorisation when accessing a service
   * default: null - no auth and open access to all!
   */
  fun withAuthProvider(authProvider: AuthProvider?): JsonRPCServerBuilder {
    this.authProvider = authProvider
    return this
  }

  fun withHttpServerOptions(httpServerOptions: HttpServerOptions) : JsonRPCServerBuilder {
    this.httpServerOptions = httpServerOptions
    return this
  }

  /**
   * build the server
   * don't forget to start the server using [JsonRPCServerBuilder.build]
   */
  fun build() : JsonRPCServer {
    return JsonRPCServer.createJsonRpcServer(this)
  }
}


class JsonRPCServer private constructor(private val builder: JsonRPCServerBuilder) {
  companion object {
    init {
      io.bluebank.braid.core.json.BraidJacksonInit.init()
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
        vertx!!.deployVerticle(JsonRPCVerticle(rootPath, services, port, authProvider, httpServerOptions)) {
          if (it.failed()) {
            println("failed to deploy: ${it.cause().message}")
            callback(failedFuture(it.cause()))
          } else {
            deploymentId = it.result()
            val protocol = if (builder.httpServerOptions.isSsl) "https" else "http"
            println("server mounted on ${protocol}://localhost:$port$rootPath")
            println("sockjs JsonRPC server mounted on ${protocol}://localhost:$port${rootPath}jsonrpc/")
            println("editor mounted on ${protocol}://localhost:$port")
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

