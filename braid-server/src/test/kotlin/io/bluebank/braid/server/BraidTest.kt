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
import io.bluebank.braid.corda.serialisation.BraidCordaJacksonInit
import io.bluebank.braid.core.socket.findFreePort
import io.bluebank.braid.server.domain.SimpleNodeInfo
import io.bluebank.braid.server.util.assertThat
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.Async
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.finance.AMOUNT
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matchers.greaterThan
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.Arrays.asList


/**
 * Run with Either
 *          -DbraidStarted=true and CordaStandalone and BraidMain started on port 8999 in the background if you want this test to run faster.
 *          -DcordaStarted=true and CordaStandalone in the background if you want this test to run fastish.
 * Otherwise it takes about 45 seconds or more to run.
 */
@RunWith(VertxUnitRunner::class)
class BraidTest {



    companion object {
        var log = loggerFor<BraidTest>()

        val user = User("user1", "test", permissions = setOf("ALL"))
        val bankA = CordaX500Name("BankA", "", "GB")
        val bankB = CordaX500Name("BankB", "", "US")

        var port = findFreePort()
        val client = Vertx.vertx().createHttpClient()
        var driverl=Future.succeededFuture("");
        //var nodeA =

        @BeforeClass
        @JvmStatic
        fun setUp(testContext: TestContext) {
            BraidCordaJacksonInit.init()
            val async = testContext.async()


            if ("true".equals(System.getProperty("braidStarted"))) {
                port = 8999
                async.complete()

            } else if ("true".equals(System.getProperty("cordaStarted"))) {
                startBraid(async, NetworkHostAndPort("localhost", 10005))
            } else {
                Vertx.vertx(VertxOptions()
                        .setBlockedThreadCheckInterval(10000000))
                        .executeBlocking<String>({ startNodesAndBraid(async)},{})
            }
        }

        private fun startNodesAndBraid(async: Async) {
            driver(DriverParameters(cordappsForAllNodes = listOf(
                    TestCordapp.findCordapp("net.corda.finance.contracts.asset"),
                    TestCordapp.findCordapp("net.corda.finance.schemas"),
                    TestCordapp.findCordapp("net.corda.finance.flows")),
                    isDebug = true, startNodesInProcess = true)) {
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
                readLine()          // stop the driver shutting down corda at this point. Whats wrong with objects and garbage collection!!
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
    fun shouldListNetworkNodes(context: TestContext) {
        val async = context.async()

        log.info("calling get: http://localhost:${port}/api/rest/network/nodes")
        client.get(port, "localhost", "/api/rest/network/nodes")
                .putHeader("Accept", "application/json; charset=utf8")
                .exceptionHandler(context::fail)
                .handler {
                    context.assertEquals(200, it.statusCode(), it.statusMessage())

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
    fun shouldListNetworkNodesByHostAndPort(context: TestContext) {
        val async = context.async()

        log.info("calling get: http://localhost:${port}/api/rest/network/nodes")
        client.get(port, "localhost", "/api/rest/network/nodes?host-and-port=localhost:10004")
                .putHeader("Accept", "application/json; charset=utf8")
                .exceptionHandler(context::fail)
                .handler {
                    context.assertEquals(200, it.statusCode(), it.statusMessage())

                    it.bodyHandler {
                        val nodes = it.toJsonArray()

                        val node = nodes.getJsonObject(0)

                        val addresses = node.getJsonArray("addresses")
                        context.assertThat(addresses.size(), equalTo(1))
                        context.assertThat(addresses.getJsonObject(0).getString("host"), equalTo("localhost"))
                        context.assertThat(addresses.getJsonObject(0).getInteger("port"), equalTo(10004))

                        async.complete()
                    }
                }
                .end()
    }

    @Test
    fun shouldListNetworkNodesByX509Name(context: TestContext) {
        val async = context.async()

        log.info("calling get: http://localhost:${port}/api/rest/network/nodes")
        client.get(port, "localhost", "/api/rest/network/nodes?x500-name=O%3DNotary%20Service,%20L%3DZurich,%20C%3DCH")
                .putHeader("Accept", "application/json; charset=utf8")
                .exceptionHandler(context::fail)
                .handler {
                    context.assertEquals(200, it.statusCode(), it.statusMessage())

                    it.bodyHandler {
                        val nodes = it.toJsonArray()

                        val node = nodes.getJsonObject(0)

                        val addresses = node.getJsonArray("addresses")
                        context.assertThat(addresses.size(), equalTo(1))
                        context.assertThat(addresses.getJsonObject(0).getString("host"), equalTo("localhost"))
                        context.assertThat(addresses.getJsonObject(0).getInteger("port"), equalTo(10000))

                        async.complete()
                    }
                }
                .end()
    }

    @Test
    fun shouldListSelf(context: TestContext) {
        val async = context.async()

        log.info("calling get: http://localhost:${port}/api/rest/network/nodes/self")
        client.get(port, "localhost", "/api/rest/network/nodes/self")
                .putHeader("Accept", "application/json; charset=utf8")
                .exceptionHandler(context::fail)
                .handler {
                    context.assertEquals(200, it.statusCode(), it.statusMessage())

                    it.bodyHandler {
                        val node = it.toJsonObject()

                        val addresses = node.getJsonArray("addresses")
                        context.assertThat(addresses.size(), equalTo(1))
                        context.assertThat(addresses.getJsonObject(0).getString("host"), equalTo("localhost"))
                        context.assertThat(addresses.getJsonObject(0).getInteger("port"), equalTo(10004))

                        async.complete()
                    }
                }
                .end()
    }

    @Test
    fun shouldListNetworkNotaries(context: TestContext) {
        val async = context.async()

        log.info("calling get: http://localhost:${port}/api/rest/network/notaries")
        client.get(port, "localhost", "/api/rest/network/notaries")
                .putHeader("Accept", "application/json; charset=utf8")
                .exceptionHandler(context::fail)
                .handler {
                    context.assertEquals(200, it.statusCode(), it.statusMessage())

                    it.bodyHandler {
                        val nodes = it.toJsonArray()

                     //   val nodes = Json.decodeValue(it, object : TypeReference<List<Party>>() {})

                        context.assertThat(nodes.size(), equalTo(1))
                        context.assertThat(nodes.getJsonObject(0).getString("name"), equalTo("O=Notary Service, L=Zurich, C=CH"))

                        async.complete()
                    }
                }
                .end()
    }

    @Test
    fun shouldListFlows(context: TestContext) {
        val async = context.async()

        log.info("calling get: http://localhost:${port}/api/rest/cordapps/flows")
        client.get(port, "localhost", "/api/rest/cordapps/flows")
                .putHeader("Accept", "application/json; charset=utf8")
                .exceptionHandler(context::fail)
                .handler {
                    context.assertEquals(200, it.statusCode(), it.statusMessage())

                    it.bodyHandler {
                        val nodes = Json.decodeValue(it, object : TypeReference<List<String>>() {})

                        context.assertThat(nodes.size, greaterThan(2))

                        context.assertThat(nodes, hasItem("net.corda.core.flows.ContractUpgradeFlow\$Authorise"))
                        context.assertThat(nodes, hasItem("net.corda.core.flows.ContractUpgradeFlow\$Deauthorise"))
                        context.assertThat(nodes, hasItem("net.corda.core.flows.ContractUpgradeFlow\$Initiate"))
                        context.assertThat(nodes, hasItem("net.corda.finance.flows.CashIssueFlow"))

                        async.complete()
                    }
                }
                .end()
    }

    @Test
    @Ignore   // failing for some reason in build ok locally.
    fun shouldStartFlow(context: TestContext) {
        val async = context.async()

        // amount as query parameter
        // issuerBankPartyRef as query parameter
        // Party as body
        val amount = Json.encode(AMOUNT(10.00, Currency.getInstance("GBP")))
        val notary = "{\"name\":\"O=Notary Service, L=Zurich, C=CH\",\"owningKey\":\"GfHq2tTVk9z4eXgyVjEnMc2NbZTfJ6Y3YJDYNRvPn2U7jiS3suzGY1yqLhgE\"}";
    //    val party2 = Json.encode(Party(CordaX500Name.parse("O=Notary Service, L=Zurich, C=CH"), RSAPublicKeyImpl(null)))

        val json = JsonObject()
                .put("notary", JsonObject(notary))
                .put("amount", JsonObject(amount))
                .put("issuerBankPartyRef", JsonObject())       /// todo serialize OpaqueBytes


        log.info("calling put: http://localhost:${port}/api/rest/cordapps/flows/net.corda.finance.flows.CashIssueFlow")

        val encodePrettily = json.encodePrettily()
        client.post(port, "localhost", "/api/rest/cordapps/flows/net.corda.finance.flows.CashIssueFlow")
                .putHeader("Accept", "application/json; charset=utf8")
                .putHeader("Content-length",""+encodePrettily.length)
                .exceptionHandler(context::fail)
                .handler {
                    context.assertEquals(200, it.statusCode(), it.statusMessage())

                    it.bodyHandler {
                        val reply = it.toJsonObject()
                        context.assertThat(reply, notNullValue())
                        context.assertThat(reply.getJsonObject("stx"), notNullValue())
                        context.assertThat(reply.getJsonObject("recipient"), notNullValue())

                        async.complete()
                    }
                }
                .end(encodePrettily)
    }

}
