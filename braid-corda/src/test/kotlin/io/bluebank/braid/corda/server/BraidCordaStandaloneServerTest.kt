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

import com.fasterxml.jackson.core.type.TypeReference
import io.bluebank.braid.corda.BraidCordaJacksonSwaggerInit
import io.bluebank.braid.corda.services.SimpleNodeInfo
import io.bluebank.braid.corda.util.VertxMatcher.vertxAssertThat
import io.bluebank.braid.core.async.catch
import io.bluebank.braid.core.async.onSuccess
import io.bluebank.braid.core.http.body
import io.bluebank.braid.core.http.getFuture
import io.bluebank.braid.core.socket.findFreePort
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.http.HttpClientOptions
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
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import java.util.Arrays.asList
import javax.ws.rs.core.Response

/**
 * Run with Either
 *          -DbraidStarted=true and CordaStandalone and BraidMain started on port 8999 in the background if you want this test to run faster.
 *          -DcordaStarted=true and CordaStandalone in the background if you want this test to run fastish.
 * Otherwise it takes about 45 seconds or more to run.
 */
@Suppress("DEPRECATION")
@RunWith(VertxUnitRunner::class)
class BraidCordaStandaloneServerTest {

  companion object {
    private val log = loggerFor<BraidCordaStandaloneServerTest>()

    private val user = User("user1", "test", permissions = setOf("ALL"))
    private val bankA = CordaX500Name("BankA", "", "GB")
    private val bankB = CordaX500Name("BankB", "", "US")

    private val port = if ("true".equals(System.getProperty("braidStarted"))) 8999 else findFreePort()
    private val clientVertx = Vertx.vertx()
    private val client = clientVertx.createHttpClient(HttpClientOptions().setDefaultHost("localhost").setDefaultPort(port))
      

    @BeforeClass
    @JvmStatic
    fun beforeClass(testContext: TestContext) {
      BraidCordaJacksonSwaggerInit.init()
      val async = testContext.async()

      if ("true".equals(System.getProperty("braidStarted"))) {
        async.complete()
      } else if ("true".equals(System.getProperty("cordaStarted"))) {
        startBraid(async, NetworkHostAndPort("localhost", 10005))
      } else {
        Vertx.vertx(
          VertxOptions()
            .setBlockedThreadCheckInterval(10000000)
        )
          .executeBlocking<String>({ startNodesAndBraid(async) }, {})
      }
    }

    private fun startNodesAndBraid(async: Async) {
      driver(
        DriverParameters(
          cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("net.corda.finance.contracts.asset"),
            TestCordapp.findCordapp("net.corda.finance.schemas"),
            TestCordapp.findCordapp("net.corda.finance.flows")
          ),
          isDebug = true, startNodesInProcess = true
        )
      ) {
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

    private fun startBraid(
      async: Async,
      networkHostAndPort: NetworkHostAndPort
    ): Future<String>? {
      // compile time check that we can inherit from BraidCordaStandaloneServer
      return object : BraidCordaStandaloneServer(
        userName = "user1",
        password = "test",
        port = port,
        nodeAddress = networkHostAndPort
      ) {}
        .startServer()
        .setHandler {
          async.complete()
        }
    }

    @AfterClass
    @JvmStatic
    fun closeDown(context: TestContext) {
      client.close()
      clientVertx.close(context.asyncAssertSuccess())
    }
  }

  @Test
  fun shouldListNetworkNodes(context: TestContext) {
    val async = context.async()

    log.info("calling get: http://localhost:$port/api/rest/network/nodes")
    client.get(port, "localhost", "/api/rest/network/nodes")
      .putHeader("Accept", "application/json; charset=utf8")
      .exceptionHandler(context::fail)
      .handler {
        context.assertEquals(200, it.statusCode(), it.statusMessage())

        it.bodyHandler {
          val nodes =
            Json.decodeValue(it, object : TypeReference<List<SimpleNodeInfo>>() {})

          context.assertThat(nodes.size, equalTo(3))

          context.assertThat(
            nodes.get(0).addresses.get(0),
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

    log.info("calling get: http://localhost:$port/api/rest/network/nodes")
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
          context.assertThat(
            addresses.getJsonObject(0).getString("host"),
            equalTo("localhost")
          )
          context.assertThat(
            addresses.getJsonObject(0).getInteger("port"),
            equalTo(10004)
          )

          async.complete()
        }
      }
      .end()
  }

  @Test
  fun shouldGetPartyB(context: TestContext) {
    val cordaX500Name = CordaX500Name("PartyB", "New York", "US")

    val toString = cordaX500Name.toString()
    val encode = URLEncoder.encode(toString)
    Assert.assertThat(encode, `is`("O%3DPartyB%2C+L%3DNew+York%2C+C%3DUS"))
  }


  @Test
  fun shouldDecode(context: TestContext) {

    val encode = URLDecoder.decode("O%3DPartyB%2CL%3DNew+York%2CC%3DUS")
    val parse = CordaX500Name.parse(encode)
    val cordaX500Name = CordaX500Name("PartyB", "New York", "US")
    Assert.assertThat(parse, `is`(cordaX500Name))
  }

  @Test
  fun shouldListNetworkNodesByX509Name(context: TestContext) {
    val async = context.async()

    log.info("calling get: http://localhost:$port/api/rest/network/nodes")
    client.get(
      port,
      "localhost",
      "/api/rest/network/nodes?x500-name=O%3DNotary%20Service,%20L%3DZurich,%20C%3DCH"
    )
      .putHeader("Accept", "application/json; charset=utf8")
      .exceptionHandler(context::fail)
      .handler {
        context.assertEquals(200, it.statusCode(), it.statusMessage())

        it.bodyHandler {
          val nodes = it.toJsonArray()

          val node = nodes.getJsonObject(0)

          val addresses = node.getJsonArray("addresses")
          context.assertThat(addresses.size(), equalTo(1))
          context.assertThat(
            addresses.getJsonObject(0).getString("host"),
            equalTo("localhost")
          )
          context.assertThat(
            addresses.getJsonObject(0).getInteger("port"),
            equalTo(10000)
          )

          async.complete()
        }
      }
      .end()
  }

 @Test
  fun `should return empty list if node not found`(context: TestContext) {
    val async = context.async()

    log.info("calling get: http://localhost:$port/api/rest/network/nodes")
    client.get(
      port,
      "localhost",
      "/api/rest/network/nodes?x500-name=O%3DPartyB%2CL%3DNew+York%2CC%3DUS"
    )
      .putHeader("Accept", "application/json; charset=utf8")
      .exceptionHandler(context::fail)
      .handler {
        context.assertEquals(200, it.statusCode(), it.statusMessage())

        it.bodyHandler {
          val nodes = it.toJsonArray()
          context.assertThat(nodes.size(), equalTo(0))
          async.complete()
        }
      }
      .end()
  }

  @Test
  fun shouldListSelf(context: TestContext) {
    val async = context.async()

    log.info("calling get: http://localhost:$port/api/rest/network/nodes/self")
    client.get(port, "localhost", "/api/rest/network/nodes/self")
      .putHeader("Accept", "application/json; charset=utf8")
      .exceptionHandler(context::fail)
      .handler {
        context.assertEquals(200, it.statusCode(), it.statusMessage())

        it.bodyHandler {
          val node = it.toJsonObject()

          val addresses = node.getJsonArray("addresses")
          context.assertThat(addresses.size(), equalTo(1))
          context.assertThat(
            addresses.getJsonObject(0).getString("host"),
            equalTo("localhost")
          )
          context.assertThat(
            addresses.getJsonObject(0).getInteger("port"),
            equalTo(10004)
          )

          async.complete()
        }
      }
      .end()
  }

  @Test
  fun shouldListNetworkNotaries(context: TestContext) {
    val async = context.async()

    log.info("calling get: http://localhost:$port/api/rest/network/notaries")
    client.get(port, "localhost", "/api/rest/network/notaries")
      .putHeader("Accept", "application/json; charset=utf8")
      .exceptionHandler(context::fail)
      .handler {
        context.assertEquals(200, it.statusCode(), it.statusMessage())

        it.bodyHandler {
          val nodes = it.toJsonArray()

          //   val nodes = Json.decodeValue(it, object : TypeReference<List<Party>>() {})

          context.assertThat(nodes.size(), equalTo(1))
          context.assertThat(
            nodes.getJsonObject(0).getString("name"),
            equalTo("O=Notary Service, L=Zurich, C=CH")
          )
//                        context.assertThat(nodes.getJsonObject(0).getString("owningKey"), equalTo("GfHq2tTVk9z4eXgySzYjYp2YsTewf2FHZCb1Ls31XPzG7Hy2hRUeM8cFaFu4"))

          async.complete()
        }
      }
      .end()
  }

  @Test
  fun shouldListFlows(context: TestContext) {
    val async = context.async()
    log.info("calling get: http://localhost:$port/api/rest/cordapps/flows")
    client.getFuture("/api/rest/cordapps/corda-core/flows")
      .compose { it.body<List<String>>() }
      .onSuccess { flows ->
        context.assertThat(
          flows,
          hasItem("net.corda.core.flows.ContractUpgradeFlow\$Authorise")
        )
        context.assertThat(
          flows,
          hasItem("net.corda.core.flows.ContractUpgradeFlow\$Deauthorise")
        )
      }
      .compose { client.getFuture("/api/rest/cordapps/corda-finance-workflows/flows") }
      .compose { it.body<List<String>>() }
      .onSuccess { flows ->
        context.assertThat(flows, hasItem("net.corda.finance.flows.CashIssueFlow"))
      }
      .onSuccess { async.complete() }
      .catch(context::fail)
  }

  @Test
  fun shouldStartFlow(context: TestContext) {
    val async = context.async()

    getNotary().map {
      val notary = it

      val json = JsonObject()
        .put("notary", notary)
        .put("amount", JsonObject(Json.encode(AMOUNT(10.00, Currency.getInstance("GBP")))))
        .put("issuerBankPartyRef", "AABBCC")

      val path = "/api/rest/cordapps/corda-finance-workflows/flows/net.corda.finance.flows.CashIssueFlow"
      log.info("calling post: http://localhost:$port$path")

      val encodePrettily = json.encodePrettily()
      client.post(port, "localhost", path)
        .putHeader("Accept", "application/json; charset=utf8")
        .putHeader("Content-length", "" + encodePrettily.length)
        .exceptionHandler(context::fail)
        .handler {
          context.assertEquals(200, it.statusCode(), it.statusMessage())

          it.bodyHandler {
            val reply = it.toJsonObject()
            log.info("reply:" + reply.encodePrettily())
            context.assertThat(reply, notNullValue())
            context.assertThat(reply.getJsonObject("stx"), notNullValue())
            context.assertThat(reply.getJsonObject("recipient"), notNullValue())

            async.complete()
          }
        }
        .end(encodePrettily)
    }
  }

  @Test
  fun shouldReplyWithDecentErrorOnBadJson(context: TestContext) {
    val async = context.async()

    getNotary().map { jsonObject ->
      val notary = jsonObject

      val json = JsonObject()
        .put("notary", notary)
        .put("amount", JsonObject(Json.encode(AMOUNT(10.00, Currency.getInstance("GBP")))))
        .put("issuerBaaaaaankPartyRef", JsonObject().put("junk", "sdsa"))

      val path =
        "/api/rest/cordapps/corda-finance-workflows/flows/net.corda.finance.flows.CashIssueFlow"
      log.info("calling post: http://localhost:$port$path")

      val encodePrettily = json.encodePrettily()
      client.post(port, "localhost", path)
        .putHeader("Accept", "application/json; charset=utf8")
        .putHeader("Content-length", "" + encodePrettily.length)
        .exceptionHandler(context::fail)
        .handler { clientResponse ->
          context.assertEquals(
            Response.Status.BAD_REQUEST.statusCode,
            clientResponse.statusCode(),
            clientResponse.statusMessage()
          )

          clientResponse.bodyHandler {
            val reply = it.toString()
            log.info("reply: $reply")
            context.assertThat(reply, containsString("issuerBaaaaaankPartyRef"))

            async.complete()
          }
        }
        .end(encodePrettily)
    }
  }

  @Test
  fun `should list cordapps`(context: TestContext) {
    val async = context.async()
    val path = "/api/rest/cordapps"
    client.getFuture(path)
      .compose { it.body<List<String>>() }
      .onSuccess { list ->
        context.assertTrue(list.contains("corda-core"))
        context.assertTrue(list.contains("corda-finance-contracts"))
        context.assertTrue(list.contains("corda-finance-workflows"))
      }
      .onSuccess { async.complete() }
      .catch { context.fail(it) }
  }

  private fun getNotary(): Future<JsonObject> {
    val result = Future.future<JsonObject>()
    client.get(port, "localhost", "/api/rest/network/notaries")
      .putHeader("Accept", "application/json; charset=utf8")
      .handler {

        it.bodyHandler {
          val nodes = it.toJsonArray()

          //   val nodes = Json.decodeValue(it, object : TypeReference<List<Party>>() {})

          result.complete(nodes.getJsonObject(0))
        }
      }
      .end()
    return result;
  }



  @Test
  fun `should query the vault`(context: TestContext) {
    val async = context.async()



    log.info("calling get: http://localhost:${port}/api/rest/network/vault")
    client.get(port, "localhost", "/api/rest/vault/vaultQuery")
        .putHeader("Accept", "application/json; charset=utf8")
        .exceptionHandler(context::fail)
        .handler {
          context.assertEquals(200, it.statusCode(), it.statusMessage())

          it.bodyHandler {
            val nodes = it.toJsonObject()

            vertxAssertThat(context,nodes, notNullValue())

            async.complete()
          }
        }
        .end()
  }



  @Test
  fun `should query the vault by type`(context: TestContext) {
    val async = context.async()

    val json ="""
{
  "criteria" : {
    "@class" : ".QueryCriteria${'$'}VaultQueryCriteria",
    "status" : "UNCONSUMED",
    "contractStateTypes" : null,
    "stateRefs" : null,
    "notary" : null,
    "softLockingCondition" : null,
    "timeCondition" : {
      "type" : "RECORDED",
      "predicate" : {
        "@class" : ".ColumnPredicate${'$'}Between",
        "rightFromLiteral" : "2019-09-15T12:58:23.283Z",
        "rightToLiteral" : "2019-10-15T12:58:23.283Z"
      }
    },
    "relevancyStatus" : "ALL",
    "constraintTypes" : [ ],
    "constraints" : [ ],
    "participants" : null
  },
  "paging" : {
    "pageNumber" : -1,
    "pageSize" : 200
  },
  "sorting" : {
    "columns" : [ ]
  },
  "contractStateType" : "net.corda.core.contracts.ContractState"
}
"""


    log.info("calling get: http://localhost:${port}/api/rest/network/vault")
    client.post(port, "localhost", "/api/rest/vault/vaultQueryBy")
        .putHeader("Accept", "application/json; charset=utf8")
        .putHeader("Content-length", ""+json.length)
        .exceptionHandler(context::fail)
        .handler {
          context.assertEquals(200, it.statusCode(), it.statusMessage())

          it.bodyHandler {
            val nodes = it.toJsonObject()

            vertxAssertThat(context,nodes, notNullValue())

            async.complete()
          }
        }
        .end(json)
  }

}
