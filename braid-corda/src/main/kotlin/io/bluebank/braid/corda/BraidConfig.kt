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
package io.bluebank.braid.corda

import com.google.common.io.Resources
import io.bluebank.braid.core.http.HttpServerConfig.Companion.defaultServerOptions
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import net.corda.core.flows.FlowLogic
import net.corda.core.node.AppServiceHub
import net.corda.core.node.ServiceHub
import kotlin.reflect.KClass

data class BraidConfig(val port: Int = 8080,
                       val rootPath: String = "/api/",
                       val registeredFlows: Map<String, Class<out FlowLogic<*>>> = emptyMap(),
                       val services: Map<String, Any> = emptyMap(),
                       val authConstructor: ((Vertx) -> AuthProvider)? = null,
                       val httpServerOptions: HttpServerOptions = defaultServerOptions()) {

  companion object {
    private val log = loggerFor<BraidConfig>()

    @JvmStatic
    fun fromResource(resourcePath: String): BraidConfig? {
      val fullConfig = try {
        val file = Resources.toString(Resources.getResource(resourcePath), Charsets.UTF_8)
        JsonObject(file)
      } catch (err: Throwable) {
        val msg = "could not find config resource $resourcePath"
        log.warn(msg, err)
        null
      }
      return if (fullConfig != null) {
        Json.decodeValue<BraidConfig>(fullConfig.encode(), BraidConfig::class.java)
      } else {
        null
      }
    }
  }

  fun withPort(port: Int) = this.copy(port = port)
  fun withRootPath(rootPath: String) = this.copy(rootPath = rootPath)
  fun withAuthConstructor(authConstructor: ((Vertx) -> AuthProvider)) = this.copy(authConstructor = authConstructor)
  fun withHttpServerOptions(httpServerOptions: HttpServerOptions) = this.copy(httpServerOptions = httpServerOptions)
  fun withService(service: Any) = withService(service.javaClass.simpleName.decapitalize(), service)
  fun withService(name: String, service: Any) : BraidConfig {
    val map = services.toMutableMap()
    map.put(name, service)
    return this.copy(services = map)
  }
  inline fun <reified T : FlowLogic<*>> withFlow(name: String, flowClass: KClass<T>) = withFlow(name, flowClass.java)
  inline fun <reified T : FlowLogic<*>> withFlow(flowClass: KClass<T>) = withFlow(flowClass.java)

  fun <T : FlowLogic<*>> withFlow(flowClass: Class<T>) =
      withFlow(flowClass.simpleName.decapitalize(), flowClass)

  fun <T : FlowLogic<*>> withFlow(name: String, flowClass: Class<T>): BraidConfig {
    val map = registeredFlows.toMutableMap()
    map.put(name, flowClass)
    return this.copy(registeredFlows = map)
  }

  internal val protocol: String get() = if (httpServerOptions.isSsl) "https" else "http"
  fun bootstrapBraid(serviceHub: AppServiceHub) = BraidServer.bootstrapBraid(serviceHub, this)
}