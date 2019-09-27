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
package io.bluebank.braid.corda.server

import io.bluebank.braid.corda.BraidConfig
import io.bluebank.braid.corda.BraidCordaJacksonSwaggerInit
import io.bluebank.braid.corda.rest.RestConfig
import io.bluebank.braid.corda.server.flow.FlowInitiator
import io.bluebank.braid.corda.server.rpc.RPCFactory
import io.bluebank.braid.corda.server.rpc.RPCFactory.Companion.createRpcFactory
import io.bluebank.braid.corda.services.SimpleNetworkMapService
import io.bluebank.braid.corda.services.adapters.toCordaServicesAdapter
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.Future
import io.vertx.core.http.HttpServerOptions
import net.corda.core.utilities.NetworkHostAndPort

class BraidCordaStandaloneServer(
  val port: Int = 8080,
  val userName: String = "",
  val password: String = "",
  val nodeAddress: NetworkHostAndPort = NetworkHostAndPort("localhost", 8080),
  val openApiVersion: Int = 2
) {
  companion object {
    private val log = loggerFor<BraidCordaStandaloneServer>()
    init {
      BraidCordaJacksonSwaggerInit.init()
    }
  }

  fun startServer(): Future<String> {
    log.info("Starting Braid on port: $port")
    val result = Future.future<String>()
    BraidConfig()
      .withPort(port)
      .withHttpServerOptions(HttpServerOptions().apply { isSsl = false })
      .withRestConfig(restConfig(createRpcFactory(userName, password, nodeAddress), openApiVersion))
      .bootstrapBraid(null, result)
    //addShutdownHook {  }
    return result
  }

  fun restConfig(rpc: RPCFactory, openApiVersion: Int = 2): RestConfig {
    val classLoader = Thread.currentThread().contextClassLoader
    val cordappsScanner = CordappScanner(classLoader)

    val cordaServicesAdapter = rpc.toCordaServicesAdapter()
    val flowInitiator = FlowInitiator(cordaServicesAdapter)
    val networkService = SimpleNetworkMapService(cordaServicesAdapter)
    return RestConfig()
      .withOpenApiVersion(openApiVersion)
      .withPaths {
        group("network") {
          get("/network/nodes", networkService::nodes)
          get("/network/notaries", networkService::notaries)
          get("/network/nodes/self", networkService::myNodeInfo)
        }
        group("cordapps") {
          get("/cordapps", cordappsScanner::cordapps)
          get("/cordapps/:cordapp/flows", cordappsScanner::flowsForCordapp)
          try {
            cordappsScanner.cordappAndFlowList()
            cordappsScanner.cordappAndFlowList().forEach { (cordapp, flowClass) ->
              try {
                val path = "/cordapps/$cordapp/flows/${flowClass.java.name}"
                log.info("registering: $path")
                post(path, flowInitiator.getInitiator(flowClass))
              } catch (e: Throwable) {
                log.warn("unable to register flow:${flowClass.java.name}", e);
              }
            }
          } catch (e: Throwable) {
            log.error("failed to register flows", e)
          }
        }
      }
  }
}




