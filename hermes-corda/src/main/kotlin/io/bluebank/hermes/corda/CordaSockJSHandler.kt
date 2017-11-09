package io.bluebank.hermes.corda

import io.bluebank.hermes.corda.services.CordaFlowServiceExecutor
import io.bluebank.hermes.corda.services.SimpleNetworkMapService
import io.bluebank.hermes.core.jsonrpc.JsonRPCMounter
import io.bluebank.hermes.core.jsonrpc.JsonRPCRequest
import io.bluebank.hermes.core.jsonrpc.JsonRPCResponse
import io.bluebank.hermes.core.service.ConcreteServiceExecutor
import io.bluebank.hermes.core.service.ServiceExecutor
import io.bluebank.hermes.core.socket.AuthenticatedSocket
import io.bluebank.hermes.core.socket.SockJSSocketWrapper
import io.bluebank.hermes.core.socket.Socket
import io.bluebank.hermes.core.socket.TypedSocket
import io.bluebank.hermes.core.socket.impl.AuthenticatedSocketImpl
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
      // mount each service
      println("Mounting hermes services...")
      REGISTERED_HANDLERS
          .map {
            println("mounting ${it.key} to http://localhost:${config.port}${config.rootPath}${it.key}")
            it
          }
          .map {
            "${config.rootPath}${it.key}/*"
          }
          .forEach {
            router.route(it).handler(sockJSHandler)
          }
    }

    private fun createNetworkMapService(services: ServiceHubInternal, config: HermesConfig) : ServiceExecutor {
      return ConcreteServiceExecutor(SimpleNetworkMapService(services, config))
    }

    private fun createFlowService(services: ServiceHubInternal, config: HermesConfig): ServiceExecutor {
      return CordaFlowServiceExecutor(services, config)
    }
  }

  private val authProvider = config.authConstructor?.invoke(vertx)
  private val serviceMap = REGISTERED_HANDLERS.map { it.key to it.value(services, config) }.toMap()
  private val pathRegEx = Regex("${config.rootPath.replace("/", "\\/")}([^\\/]+).*")

  override fun handle(socket: SockJSSocket) {
    val serviceName = pathRegEx.matchEntire(socket.uri())?.groupValues?.get(1) ?: ""
    val service = serviceMap[serviceName]
    if (service != null) {
      handleKnownService(socket, authProvider, service)
    } else {
      handleUnknownService(socket, serviceName)
    }
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
    return with(SockJSSocketWrapper.create(socket)) {
      if (authProvider != null) { // handle the case where we need to authenticate
        appendAuthProcessor(authProvider)
      } else {
        this
      }
    }
  }

  private fun SockJSSocketWrapper.appendAuthProcessor(authProvider: AuthProvider): AuthenticatedSocketImpl {
    val authenticatedSocket = AuthenticatedSocket.create(authProvider)
    addListener(authenticatedSocket)
    return authenticatedSocket
  }
}

fun Router.setupSockJSHandler(vertx: Vertx, services: ServiceHubInternal, config: HermesConfig): Router {
  CordaSockJSHandler.setupSockJSHandler(this, vertx, services, config)
  return this
}