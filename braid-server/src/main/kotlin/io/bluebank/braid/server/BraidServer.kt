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
import io.bluebank.braid.server.domain.SimpleNodeInfo
import io.bluebank.braid.server.domain.toSimpleNodeInfo
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.vertx.core.AsyncResult
import io.vertx.core.http.HttpServerOptions
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import java.util.stream.Collectors.toList
import javax.ws.rs.QueryParam

class BraidServer(val rpc: CordaRPCOps) {
    companion object {
        private val log = loggerFor<Braid>()
    }


    public fun bootstrapBraid(port: Int) {
        log.info("Starting Braid on port:" + port)
        createBraidConfig(port).bootstrapBraid(null, io.vertx.core.Handler { handleResult(it) })
    }

    private fun createBraidConfig(port: Int): BraidConfig {
        return BraidConfig()
                // .withFlow(IssueObligation.Initiator::class)
                .withPort(port)
                .withHttpServerOptions(HttpServerOptions().apply { isSsl = false })
                .withRestConfig(RestConfig()
                        .withPaths {
                            group("network") {
                                get("/network/nodes", this@BraidServer::nodes)
                                get("/network/notaries", this@BraidServer::notaries)
                                get("/network/nodes/self", this@BraidServer::nodeInfo)
                            }
//                            group("cordapps") {
//                                post("/cordapps/obligation-cordapp/flows/issue-obligation", this@BraidServer::issueObligation)
//                            }
                        })
    }

    private fun handleResult(it: AsyncResult<String>) {
        if (it.failed()) {
            log.error("failed to start braid", it.cause())
        }
    }


    @ApiOperation(value = "Retrieves all nodes if neither query parameter is supplied. Otherwise returns a list of one node matching the supplied query parameter.")
    fun nodeInfo(
            @ApiParam(value = "[host]:[port] for the Corda P2P of the node", example = "localhost:10000") @QueryParam(value = "host-and-port") hostAndPort: String? = null,
            @ApiParam(value = "the X500 name for the node", example = "O=PartyB, L=New York, C=US") @QueryParam(value = "x500-name") x500Name: String? = null
    ): SimpleNodeInfo {
        return rpc.nodeInfo().toSimpleNodeInfo()
    }


    @ApiOperation(value = "Retrieves all nodes if neither query parameter is supplied. Otherwise returns a list of one node matching the supplied query parameter.")
    fun nodes(
            @ApiParam(value = "[host]:[port] for the Corda P2P of the node", example = "localhost:10000") @QueryParam(value = "host-and-port") hostAndPort: String? = null,
            @ApiParam(value = "the X500 name for the node", example = "O=PartyB, L=New York, C=US") @QueryParam(value = "x500-name") x500Name: String? = null
    ): List<SimpleNodeInfo> {
        return when {
            hostAndPort?.isNotEmpty() ?: false -> {
                val address = NetworkHostAndPort.parse(hostAndPort!!)
                rpc.networkMapSnapshot().stream()
                        .filter { node -> node.addresses.contains(address) }
                        .map { node -> node.toSimpleNodeInfo() }
                        .collect(toList())
            }
            x500Name?.isNotEmpty() ?: false -> {
                val x500Name1 = CordaX500Name.parse(x500Name!!)
                val party = rpc.wellKnownPartyFromX500Name(x500Name1)
                listOfNotNull(rpc.nodeInfoFromParty(party!!)?.toSimpleNodeInfo())
            }
            else -> rpc.networkMapSnapshot().stream().map { node -> node.toSimpleNodeInfo() }.collect(toList())
        }
    }


    // example http://localhost:8080/api/rest/network/notaries?x500-name=O%3DNotary%20Service,%20L%3DZurich,%20C%3DCH
    fun notaries(@ApiParam(value = "the X500 name for the node", example = "O=PartyB, L=New York, C=US") @QueryParam(value = "x500-name") x500Name: String? = null): List<Party> {
        return when {
            x500Name?.isNotEmpty() ?: false -> listOfNotNull(rpc.notaryPartyFromX500Name(CordaX500Name.parse(x500Name!!)))
            else -> rpc.notaryIdentities()
        }
    }
}