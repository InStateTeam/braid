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

import io.bluebank.braid.corda.BraidConfig
import io.bluebank.braid.corda.rest.RestConfig
import io.bluebank.braid.corda.serialisation.BraidCordaJacksonInit
import io.bluebank.braid.corda.swagger.CustomModelConverters
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.utils.toCordappName
import io.bluebank.braid.server.flow.StartableByRPCFinder.Companion.rpcClasses
import io.bluebank.braid.server.rpc.*
import io.bluebank.braid.server.rpc.RPCFactory.Companion.createRpcFactory
import io.vertx.core.Future
import io.vertx.core.http.HttpServerOptions
import net.corda.core.utilities.NetworkHostAndPort

data class Braid(
  val port: Int = 8080,
  val userName: String = "",
  val password: String = "",
  val nodeAddress: NetworkHostAndPort = NetworkHostAndPort("localhost", 8080)
) {
  companion object {
    init {
      BraidCordaJacksonInit.init()
      CustomModelConverters.init()
    }
    fun init() {
      // will on lazy basis invoke the Jackson and ModelConverters init
    }
  }

  fun startServer(): Future<String> {
      log.info("Starting Braid on port: $port")
      val result = Future.future<String>()
      BraidConfig()
        // .withFlow(IssueObligation.Initiator::class)
        .withPort(port)
        .withHttpServerOptions(HttpServerOptions().apply { isSsl = false })
        .withRestConfig(restConfig(createRpcFactory(userName, password, nodeAddress)))
        .bootstrapBraid(null, result)
      //addShutdownHook {  }
      return result
  }

  fun restConfig(rpc: RPCFactory): RestConfig {
    val classLoader = Thread.currentThread().contextClassLoader
    val flowInitiator = FlowInitiator(rpc)
    val rpcClasses = rpcClasses(classLoader)
    return RestConfig()
      .withPaths {
        group("network") {
          get("/network/nodes", NetworkService(rpc)::nodes)
          get("/network/notaries", NetworkService(rpc)::notaries)
          get("/network/nodes/self", NetworkService(rpc)::nodeInfo)
          get("/network/vault", VaultService(rpc)::vaultQueryBy)

        }
        group("cordapps") {
          get("/cordapps/flows", FlowService(rpc)::flows)
          rpcClasses.forEach { kotlinFlowClass ->
            try {
              val cordappName =
                kotlinFlowClass.java.protectionDomain.codeSource.location.toCordappName()
              val path = "/cordapps/$cordappName/flows/${kotlinFlowClass.java.name}"
              log.info("registering: $path")
              post(path, flowInitiator.getInitiator(kotlinFlowClass))
            } catch (e: Throwable) {
              log.warn("Unable to register flow:${kotlinFlowClass.java.name}", e);
            }
          }
        }
      }
  }
}

//fun issueObligation(params: IssueObligationInitiatorParameters): Future<SignedTransaction> {
//    val amount = Amount.parseCurrency(params.amount)
//    val lender = r.identityService.wellKnownPartyFromX500Name(CordaX500Name.parse(params.lender)) ?: error("lender not found ${params.lender}")
//    return serviceHub.startFlow(IssueObligation.Initiator(amount, lender,  params.anonymous)).returnValue.toVertxFuture()
//}

private val log = loggerFor<Braid>()
