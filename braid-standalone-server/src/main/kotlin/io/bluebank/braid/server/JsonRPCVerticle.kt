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
package io.bluebank.braid.server

import io.bluebank.braid.core.http.setupAllowAnyCORS
import io.bluebank.braid.core.http.setupOptionsMethod
import io.bluebank.braid.core.http.write
import io.bluebank.braid.core.jsonrpc.JsonRPCMounter
import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.jsonrpc.JsonRPCResponse
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.meta.ServiceDescriptor
import io.bluebank.braid.core.meta.defaultServiceEndpoint
import io.bluebank.braid.core.reflection.serviceName
import io.bluebank.braid.core.security.AuthenticatedSocket
import io.bluebank.braid.core.service.ConcreteServiceExecutor
import io.bluebank.braid.core.service.ServiceExecutor
import io.bluebank.braid.core.socket.SockJSSocketWrapper
import io.bluebank.braid.core.socket.TypedSocket
import io.bluebank.braid.server.services.CompositeExecutor
import io.bluebank.braid.server.services.JavascriptExecutor
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.handler.sockjs.SockJSSocket

class JsonRPCVerticle(
  private val rootPath: String, val services: List<Any>, val port: Int,
  private val authProvider: AuthProvider?,
  private val httpServerOptions: HttpServerOptions
) : AbstractVerticle() {

  companion object {
    private val logger = loggerFor<JsonRPCVerticle>()
  }

  val serviceMap: MutableMap<String, ServiceExecutor> by lazy {
    val serviceNames = services.map { getServiceName(it) }
    val jsOnlyServices =
      JavascriptExecutor.queryServiceNames(vertx).filter { !serviceNames.contains(it) }
    val mutableServiceMap = mutableMapOf<String, ServiceExecutor>()
    services.map { getServiceName(it) to wrapConcreteService(it) }.forEach {
      mutableServiceMap[it.first] = it.second
    }
    jsOnlyServices.map { it to JavascriptExecutor(vertx, it) }.forEach {
      mutableServiceMap[it.first] = it.second
    }
    mutableServiceMap
  }

  private lateinit var router: Router
  private lateinit var sockJSHandler: SockJSHandler

  override fun start(startFuture: Future<Void>) {
    router = setupRouter()
    setupWebserver(router, startFuture)
  }

  private fun wrapConcreteService(service: Any): ServiceExecutor {
    return CompositeExecutor(
      JavascriptExecutor(vertx, getServiceName(service)),
      ConcreteServiceExecutor(service)
    )
  }

  private fun ServiceExecutor.getJavascriptExecutor(): JavascriptExecutor {
    return when (this) {
      is CompositeExecutor -> {
        executors
          .filter { it is JavascriptExecutor }
          .map { it as JavascriptExecutor }
          .firstOrNull() ?: throw RuntimeException("cannot find javascript executor")
      }
      is JavascriptExecutor -> this
      else -> throw RuntimeException("found executor is not a ${JavascriptExecutor::class.simpleName} or doesn't contain one")
    }
  }

  private fun ServiceExecutor.getConcreteExecutor(): ConcreteServiceExecutor? {
    return when (this) {
      is CompositeExecutor -> {
        executors.filter { it is ConcreteServiceExecutor }
          .map { it as ConcreteServiceExecutor }
          .firstOrNull()
      }
      is ConcreteServiceExecutor -> this
      else -> null
    }
  }

  private fun getJavascriptExecutorForService(serviceName: String): JavascriptExecutor {
    return serviceMap.computeIfAbsent(serviceName) {
      bindServiceSockJSHandler(serviceName)
      JavascriptExecutor(vertx, serviceName)
    }.getJavascriptExecutor()
  }

  private fun getJavaExecutorForService(serviceName: String): ConcreteServiceExecutor? {
    val service = serviceMap[serviceName] ?: return null
    return service.getConcreteExecutor()
  }

  private fun setupWebserver(router: Router, startFuture: Future<Void>) {
    vertx.createHttpServer(httpServerOptions.withCompatibleWebsockets())
      .requestHandler(router)
      .listen(port) {
        if (it.succeeded()) {
          logger.info("started on port $port")
          startFuture.complete()
        } else {
          logger.error("failed to start because", it.cause())
          startFuture.fail(it.cause())
        }
      }
  }

  private fun HttpServerOptions.withCompatibleWebsockets(): HttpServerOptions {
    this.websocketSubProtocols = "undefined"
    return this
  }

  private val servicesRouter: Router = Router.router(vertx)

  private fun setupRouter(): Router {
    val router = Router.router(vertx)
    router.setupAllowAnyCORS()
    router.setupOptionsMethod()
    setupSockJS()
    servicesRouter.post().handler(BodyHandler.create())
    servicesRouter.get("/").handler { it.getServiceList() }
    servicesRouter.get("/:serviceId").handler { it.getService(it.pathParam("serviceId")) }
    servicesRouter.get("/:serviceId/script")
      .handler { it.getServiceScript(it.pathParam("serviceId")) }
    servicesRouter.post("/:serviceId/script")
      .handler { it.saveServiceScript(it.pathParam("serviceId"), it.bodyAsString) }
    servicesRouter.delete("/:serviceId")
      .handler { it.deleteService(it.pathParam("serviceId")) }
    servicesRouter.get("/:serviceId/java")
      .handler { it.getJavaImplementationHeaders(it.pathParam("serviceId")) }
    router.mountSubRouter(rootPath, servicesRouter)
    router.get()
      .last()
      .handler(
        StaticHandler.create("editor-web", JsonRPCVerticle::class.java.classLoader)
          .setCachingEnabled(false)
          .setMaxCacheSize(1)
          .setCacheEntryTimeout(1)
      )
    return router
  }

  private fun RoutingContext.getServiceList() {
    val sm = ServiceDescriptor.createServiceDescriptors(rootPath, serviceMap.keys)
    write(sm)
  }

  private fun RoutingContext.getService(serviceName: String) {
    data class ServiceDocumentation(
      val java: String,
      val script: String,
      val endpoint: String
    )

    val docs = ServiceDocumentation(
      "$rootPath$serviceName/java",
      "$rootPath$serviceName/script",
      defaultServiceEndpoint(rootPath, serviceName)
    )
    write(docs)
  }

  private fun RoutingContext.getServiceScript(serviceName: String) {
    val script = getJavascriptExecutorForService(serviceName).getScript()

    response()
      .putHeader(HttpHeaders.CONTENT_TYPE, "text/javascript")
      .putHeader(HttpHeaders.CONTENT_LENGTH, script.length().toString())
      .end(script)
  }

  private fun RoutingContext.deleteService(serviceName: String) {
    val service = serviceMap[serviceName]
    if (service == null) {
      response().setStatusMessage("no service called $serviceName").end()
      return
    }
    getJavascriptExecutorForService(serviceName).deleteScript()
    if (service is CompositeExecutor) {
      response()
        .setStatusMessage("cannot delete java service $serviceName, but have deleted JS extension script")
        .end()
      return
    }
    serviceMap.remove(serviceName)
    val searchPath = sockPath(serviceName).dropLast(1) // remove the *
    router.routes.filter { it.path == searchPath }.forEach {
      logger.info("remove route $it")
      it.remove()
    }
    write("done")
  }

  private fun sockPath(serviceName: String) = "$rootPath$serviceName/*"

  private fun RoutingContext.getJavaImplementationHeaders(serviceName: String) {
    val service = getJavaExecutorForService(serviceName)

    if (service == null) {
      write("")
      return
    } else {
      write(service.getStubs())
    }
  }

  private fun RoutingContext.saveServiceScript(serviceName: String, script: String) {
    val service = getJavascriptExecutorForService(serviceName)
    try {
      service.updateScript(script)
      response().end()
    } catch (err: Throwable) {
      write(err)
    }
  }

  private fun socketHandler(socket: SockJSSocket) {
    val re = Regex("${rootPath.replace("/", "\\/")}([^\\/]+).*")
    val serviceName = re.matchEntire(socket.uri())?.groupValues?.get(1) ?: ""

    val service = serviceMap[serviceName]
    if (service != null) {
      // TODO: the pipeline setup is complex. rework this to ease comprehension
      // the slight gotcha is that the service may or may not be authenticated
      // perhaps all services should be authenticated?
      val sockWrapper = with(SockJSSocketWrapper.create(socket, vertx)) {
        if (authProvider != null) {
          val authenticatedSocket = AuthenticatedSocket.create(authProvider)
          this.addListener(authenticatedSocket)
          authenticatedSocket
        } else {
          this
        }
      }

      val rpcSocket = TypedSocket.create<JsonRPCRequest, JsonRPCResponse>()
      sockWrapper.addListener(rpcSocket)
      val mount = JsonRPCMounter(service, vertx)
      rpcSocket.addListener(mount)
    } else {
      socket.write("cannot find service $service")
      socket.close()
    }
  }

  private fun getServiceName(service: Any): String {
    return service.javaClass.serviceName()
  }

  private fun setupSockJS() {
    sockJSHandler = SockJSHandler.create(vertx)
    sockJSHandler.socketHandler(this::socketHandler)
    // mount each service

    servicesRouter.get("/:serviceId/braid/info").handler {
      val serviceId = it.pathParam("serviceId")
      if (serviceMap.contains(serviceId)) {
        it.next()
      } else {
        it.response()
          .setStatusMessage("""Braid: Service '$serviceId' does not exist. Click here to create it http://localhost:8080""".trimMargin())
          .setStatusCode(404)
          .end()
      }
    }
    serviceMap.keys.forEach {
      bindServiceSockJSHandler(it)
    }
  }

  private fun bindServiceSockJSHandler(serviceName: String) {
    servicesRouter.route("/$serviceName/braid/*").handler(sockJSHandler)
  }
}