package io.bluebank.hermes.corda

import io.bluebank.hermes.corda.services.CordaFlowServiceExecutor
import io.bluebank.hermes.corda.services.SimpleNetworkMapService
import io.bluebank.hermes.core.http.write
import io.bluebank.hermes.core.jsonrpc.JsonRPCMounter
import io.bluebank.hermes.core.jsonrpc.JsonRPCRequest
import io.bluebank.hermes.core.jsonrpc.JsonRPCResponse
import io.bluebank.hermes.core.service.ConcreteServiceExecutor
import io.bluebank.hermes.core.service.MethodDescriptor
import io.bluebank.hermes.core.service.ServiceExecutor
import io.bluebank.hermes.core.socket.AuthenticatedSocket
import io.bluebank.hermes.core.socket.SockJSSocketWrapper
import io.bluebank.hermes.core.socket.Socket
import io.bluebank.hermes.core.socket.TypedSocket
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.handler.sockjs.SockJSSocket
import net.corda.node.services.api.ServiceHubInternal


class CordaSockJSHandler private constructor(vertx: Vertx, services: ServiceHubInternal, config: HermesConfig)
  : Handler<SockJSSocket> {

  companion object {
    private val REGISTERED_HANDLERS = mapOf(
        "network" to this::createNetworkMapService,
        "flows" to this::createFlowService
    )

    fun setupSockJSHandler(router: Router, vertx: Vertx, services: ServiceHubInternal, config: HermesConfig) {
      val sockJSHandler = SockJSHandler.create(vertx)
      val handler = CordaSockJSHandler(vertx, services, config)
      sockJSHandler.socketHandler(handler)
      val protocol = if (config.httpServerOptions.isSsl) "https" else "http"

      // mount each service
      println("Mounting hermes services...")

      registerCoreServices(protocol, config, router, sockJSHandler)
      registerCustomService(config, protocol, router, sockJSHandler)
      router.get("${config.rootPath}doc/").handler {
        val services = REGISTERED_HANDLERS.map { it.key } + config.services.map { it.key }
        it.write(services)
      }
      router.get("${config.rootPath}doc/:serviceName").handler {
        val serviceName = it.pathParam("serviceName");
        val serviceDoc = handler.getDocumentation()[serviceName]
        if (serviceDoc != null) {
          it.write(serviceDoc)
        } else {
          it.write(RuntimeException("could not find service $serviceName"))
        }
      }
    }

    private fun registerCustomService(config: HermesConfig, protocol: String, router: Router, sockJSHandler: SockJSHandler?) {
      config.services
          .map {
            println("mounting ${it.key} to $protocol://localhost:${config.port}${config.rootPath}jsonrpc/${it.key}")
            it
          }
          .map {
            "${config.rootPath}jsonrpc/${it.key}/*"
          }
          .forEach {
            router.route(it).handler(sockJSHandler)
          }
    }

    private fun registerCoreServices(protocol: String, config: HermesConfig, router: Router, sockJSHandler: SockJSHandler?) {
      REGISTERED_HANDLERS
          .map {
            println("mounting ${it.key} to $protocol://localhost:${config.port}${config.rootPath}jsonrpc/${it.key}")
            it
          }
          .map {
            "${config.rootPath}jsonrpc/${it.key}/*"
          }
          .forEach {
            router.route(it).handler(sockJSHandler)
          }
    }

    private fun createNetworkMapService(services: ServiceHubInternal, config: HermesConfig) : ServiceExecutor =
        ConcreteServiceExecutor(SimpleNetworkMapService(services, config))

    private fun createFlowService(services: ServiceHubInternal, config: HermesConfig): ServiceExecutor =
        CordaFlowServiceExecutor(services, config)
  }

  private val authProvider = config.authConstructor?.invoke(vertx)
  private val serviceMap = REGISTERED_HANDLERS.map { it.key to it.value(services, config) }.toMap() +
      config.services.map { it.key to ConcreteServiceExecutor(it.value) }.toMap()
  private val pathRegEx = Regex("${config.rootPath.replace("/", "\\/")}jsonrpc/([^\\/]+).*")

  override fun handle(socket: SockJSSocket) {
    val serviceName = pathRegEx.matchEntire(socket.uri())?.groupValues?.get(1) ?: ""
    val service = serviceMap[serviceName]
    if (service != null) {
      handleKnownService(socket, authProvider, service)
    } else {
      handleUnknownService(socket, serviceName)
    }
  }

  fun getDocumentation() : Map<String, List<MethodDescriptor>> {
    return serviceMap.map {
      it.key to it.value.getStubs()
    }.toMap()
  }

  private fun handleUnknownService(socket: SockJSSocket, serviceName: String) {
    socket.write("cannot find service $serviceName")
    socket.close()
  }

  private fun handleKnownService(socket: SockJSSocket, authProvider: AuthProvider?, service: ServiceExecutor) {
    val sockWrapper = createSocketAdapter(socket, authProvider)
    val rpcSocket = TypedSocket.create<JsonRPCRequest, JsonRPCResponse>()
    sockWrapper.addListener(rpcSocket)
    val mount = JsonRPCMounter(service)
    rpcSocket.addListener(mount)
  }

  private fun createSocketAdapter(socket: SockJSSocket, authProvider: AuthProvider?): Socket<Buffer, Buffer> {
    val sockJSWrapper = SockJSSocketWrapper.create(socket)
    return if (authProvider == null) {
      sockJSWrapper
    } else {
      // we tag on the authenticator on the pipeline and return that
      val authenticatedSocket = AuthenticatedSocket.create(authProvider)
      sockJSWrapper.addListener(authenticatedSocket)
      authenticatedSocket
    }
  }
}

fun Router.setupSockJSHandler(vertx: Vertx, services: ServiceHubInternal, config: HermesConfig): Router {
  CordaSockJSHandler.setupSockJSHandler(this, vertx, services, config)
  return this
}