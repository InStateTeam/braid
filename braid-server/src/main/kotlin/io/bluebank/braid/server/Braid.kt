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
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.server.flow.StartableByRPCFinder.Companion.rpcClasses
import io.bluebank.braid.server.rpc.FlowInitiator
import io.bluebank.braid.server.rpc.FlowService
import io.bluebank.braid.server.rpc.NetworkService
import io.bluebank.braid.server.rpc.RPCFactory
import io.vertx.core.Future
import io.vertx.core.http.HttpServerOptions
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort


data class Braid(
        val port: Int = 8080,
        val userName: String = "",
        val password: String = "",
        val nodeAddress: String = ""
) {

    init {

    }

    fun withPort(port: Int): Braid = this.copy(port = port)

    fun withUserName(userName: String): Braid = this.copy(userName = userName)

    fun withPassword(password: String): Braid = this.copy(password = password)

    fun withNodeAddress(nodeAddress: String): Braid = this.copy(nodeAddress = nodeAddress)

    fun withNodeAddress(nodeAddress: NetworkHostAndPort): Braid = this.copy(nodeAddress = nodeAddress.toString())


    fun startServer(): Future<String> {

        log.info("Starting Braid on port:" + port)
        val result = Future.future<String>();

        RPCFactory(userName, password, nodeAddress)
                .validConnection()
                .map {
                    val rpc = it
                    BraidConfig()
                            // .withFlow(IssueObligation.Initiator::class)
                            .withPort(port)
                            .withHttpServerOptions(HttpServerOptions().apply { isSsl = false })
                            .withRestConfig(restConfig(rpc))
                            .bootstrapBraid(null, result.completer())
                }


        //addShutdownHook {  }

        return result
    }

    fun restConfig(rpc: CordaRPCOps): RestConfig {
        return RestConfig()
                .withPaths {
                    group("network") {
                        get("/network/nodes", NetworkService(rpc)::nodes)
                        get("/network/notaries", NetworkService(rpc)::notaries)
                        get("/network/nodes/self", NetworkService(rpc)::nodeInfo)
                        get("/cordapps/flows", FlowService(rpc)::flows)
                        //      get("/cordapps/flows/:flow", FlowService(rpc)::flowDetails)


                        rpcClasses().forEach({
                            try {
                                post("/cordapps/flows/${it.java.name}", FlowInitiator(rpc).getInitiator(it))
                            } catch (e: Throwable) {
                                log.error("Unable to register flow:${it.java.name}", e);
                            }
                        })
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
