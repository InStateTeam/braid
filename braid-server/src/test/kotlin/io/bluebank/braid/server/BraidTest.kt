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

import com.fasterxml.jackson.core.type.TypeReference
import io.bluebank.braid.core.socket.findFreePort
import io.bluebank.braid.server.domain.SimpleNodeInfo
import io.bluebank.braid.server.util.assertThat
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.json.Json
import io.vertx.ext.unit.Async
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.nodeapi.internal.ArtemisMessagingComponent
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.User
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matchers.greaterThan
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Arrays.asList


/**
 * Run with  -DcordaStarted=true and CordaStandalone in the background if you want this test to run fast.
 * Otherwise it takes about 45 seconds.
 */
@RunWith(VertxUnitRunner::class)
class BraidTest {
    companion object {
        var log = loggerFor<BraidTest>()

        val user = User("user1", "test", permissions = setOf("ALL"))
        val bankA = CordaX500Name("BankA", "", "GB")
        val bankB = CordaX500Name("BankB", "", "US")

        val port = findFreePort()
        val client = Vertx.vertx().createHttpClient()

        @BeforeClass
        @JvmStatic
        fun setUp(textContext: TestContext) {
            val async = textContext.async()

            if ("true".equals(System.getProperty("cordaStarted")))
                startBraid(async, NetworkHostAndPort("localhost",10005))
            else
                driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
                    // This starts two nodes simultaneously with startNode, which returns a future that completes when the node
                    // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
                    val (partyA, partyB) = listOf(
                            startNode(providedName = bankA, rpcUsers = asList(user)),
                            startNode(providedName = bankB, rpcUsers = asList(user))
                    ).map { it.getOrThrow() }

                    // This test makes an RPC call to retrieve another node's name from the network map, to verify that the
                    // nodes have started and can communicate. This is a very basic test, in practice tests would be starting
                    // flows, and verifying the states in the vault and other important metrics to ensure that your CorDapp is
                    // working as intended.
                    println("partyAHandle:$partyA.rpcAddress")
                    startBraid(async, partyA.rpcAddress)
                }
        }

        private fun startBraid(async: Async, networkHostAndPort: NetworkHostAndPort): Future<String>? {
            return Braid().withNodeAddress(networkHostAndPort)
                    .withUserName("user1")
                    .withPassword("test")
                    .withPort(port)
                    .startServer()
                    .setHandler {
                        async.complete()
                    }
        }

        @AfterClass
        @JvmStatic
        fun closeDown() {
              client.close()
        }
    }

    @Test
    fun shouldListNodes(context: TestContext) {
        val async = context.async()

        log.info("calling get: http://localhost:${port}/api/rest/network/nodes")
        client.get(port, "localhost", "/api/rest/network/nodes")
                .putHeader("Accept", "application/json; charset=utf8")
                .exceptionHandler(context::fail)
                .handler {
                    context.assertEquals(200, it.statusCode())

                    it.bodyHandler {
                        val nodes = Json.decodeValue(it, object : TypeReference<List<SimpleNodeInfo>>() {})

                        context.assertThat(nodes.size, equalTo(3))

                        context.assertThat(nodes.get(0).addresses.get(0),
                                either(equalTo(NetworkHostAndPort("localhost", 10004)))
                                        .or(equalTo(NetworkHostAndPort("localhost", 10000)))
                                        .or(equalTo(NetworkHostAndPort("localhost", 10008)))
                        )

                        async.complete()
                    }
                }
                .end()
    }

    @Test
    fun shouldListFlows(context: TestContext) {
        val async = context.async()

        log.info("calling get: http://localhost:${port}/api/rest/flows")
        client.get(port, "localhost", "/api/rest/flows")
                .putHeader("Accept", "application/json; charset=utf8")
                .exceptionHandler(context::fail)
                .handler {
                    context.assertEquals(200, it.statusCode())

                    it.bodyHandler {
                        val nodes = Json.decodeValue(it, object : TypeReference<List<String>>() {})

                        context.assertThat(nodes.size, greaterThan(2))

                        context.assertThat(nodes,hasItem("net.corda.core.flows.ContractUpgradeFlow\$Authorise"))
                        context.assertThat(nodes,hasItem("net.corda.core.flows.ContractUpgradeFlow\$Deauthorise"))
                        context.assertThat(nodes,hasItem("net.corda.core.flows.ContractUpgradeFlow\$Initiate"))

                        async.complete()
                    }
                }
                .end()
    }
}
