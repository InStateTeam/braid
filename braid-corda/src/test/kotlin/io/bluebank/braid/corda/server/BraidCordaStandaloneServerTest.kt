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

import io.bluebank.braid.corda.BraidCordaJacksonSwaggerInit
import io.bluebank.braid.corda.services.SimpleNodeInfo
import io.bluebank.braid.corda.services.vault.VaultQuery
import io.bluebank.braid.corda.util.VertxMatcher.vertxAssertThat
import io.bluebank.braid.corda.utils.toVertxFuture
import io.bluebank.braid.core.async.all
import io.bluebank.braid.core.async.catch
import io.bluebank.braid.core.async.mapUnit
import io.bluebank.braid.core.async.onSuccess
import io.bluebank.braid.core.http.body
import io.bluebank.braid.core.http.getFuture
import io.bluebank.braid.core.http.postFuture
import io.bluebank.braid.core.socket.findFreePort
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.handler.impl.HttpStatusException
import net.corda.core.contracts.ContractState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.Builder.greaterThanOrEqual
import net.corda.core.node.services.vault.QueryCriteria.VaultCustomQueryCriteria
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import net.corda.finance.AMOUNT
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.schemas.CashSchemaV1
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.internal.withTestSerializationEnvIfNotSet
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User
import org.hamcrest.CoreMatchers.*
import org.junit.AfterClass
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.System.getProperty
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*
import javax.ws.rs.core.Response
import kotlin.test.assertEquals


/**
 * Run with Either
 *          -DbraidStarted=true and CordaStandalone and BraidMain started on port 9000 in the background if you want this test to run faster.
 *          -DcordaStarted=true and CordaStandalone in the background if you want this test to run fastish.
 * Otherwise it takes about 45 seconds or more to run.
 */
@Suppress("DEPRECATION")
@RunWith(VertxUnitRunner::class)
class BraidCordaStandaloneServerTest {

  companion object {

    init {
      BraidCordaJacksonSwaggerInit.init()
    }

    private val log = loggerFor<BraidCordaStandaloneServerTest>()

    private val user = User("user1", "test", permissions = setOf("ALL"))
    private val bankA = CordaX500Name("BankA", "", "GB")
    private val bankB = CordaX500Name("BankB", "", "US")

    private val port = if ("true" == getProperty("braidStarted")) 9000 else findFreePort()
    private val clientVertx = Vertx.vertx()
    private val client = clientVertx.createHttpClient(HttpClientOptions().apply {
      defaultHost = "localhost"
      defaultPort = port
      isSsl = true
      isVerifyHost = false
      isTrustAll = true
    })
    private var driver: TestDriver? = null
    private var nodeA: NodeHandle? = null
    private var nodeB: NodeHandle? = null
    private val nodeAPort: Int
      get() {
        return when {
          nodeA != null -> nodeA!!.p2pAddress.port
          else -> 10_004
        }
      }

    private val nodeBPort: Int
      get() {
        return when {
          nodeB != null -> nodeB!!.p2pAddress.port
          else -> 10_004
        }
      }

    private val notaryAddress: NetworkHostAndPort
      get() {
        return when {
          driver != null -> driver!!.dsl.defaultNotaryHandle.nodeHandles.getOrThrow().first().p2pAddress
          else -> NetworkHostAndPort("localhost", 10_000)
        }
      }

    @BeforeClass
    @JvmStatic
    fun beforeClass(testContext: TestContext) {
      val async = testContext.async()
      val braidStarted = getBooleanProperty("braidStarted")
      val cordaStarted = getBooleanProperty("cordaStarted")
      when {
        braidStarted -> succeededFuture(Unit)
        cordaStarted -> startBraid(NetworkHostAndPort("localhost", 10005))
        else -> startNodesAndBraid()
      }
        .onSuccess { async.complete() }
        .catch { testContext.fail(it.cause) }
    }

    private fun getBooleanProperty(propertyName: String) = getProperty(propertyName)?.toBoolean() ?: false

    private fun startNodesAndBraid(): Future<Unit> {
      driver = TestDriver.driver(DriverParameters(
        cordappsForAllNodes = listOf(
          TestCordapp.findCordapp("net.corda.finance.contracts.asset"),
          TestCordapp.findCordapp("net.corda.finance.schemas"),
          TestCordapp.findCordapp("net.corda.finance.flows")
        ),
        isDebug = true, startNodesInProcess = true
      ))
      val nodeAFuture = driver!!.startNode(providedName = bankA, rpcUsers = listOf(user))
      val nodeBFuture = driver!!.startNode(providedName = bankB, rpcUsers = listOf(user))
      return all(nodeAFuture.toVertxFuture(), nodeBFuture.toVertxFuture())
        .onSuccess {
          nodeA = it.first()
          nodeB = it.last()
          println("partyAHandle:${nodeA!!.rpcAddress}")
        }
        .compose { startBraid(nodeA!!.rpcAddress) }
        .mapUnit()
    }

    private fun startBraid(networkHostAndPort: NetworkHostAndPort): Future<String> {
      // compile time check that we can inherit from BraidCordaStandaloneServer
      return BraidCordaStandaloneServer(
        userName = "user1",
        password = "test",
        port = port,
        nodeAddress = networkHostAndPort
      ).startServer()
    }

    @AfterClass
    @JvmStatic
    fun closeDown(context: TestContext) {
      if (driver != null) driver!!.close()
      client.close()
      clientVertx.close(context.asyncAssertSuccess())
    }
  }

  @Test
  fun shouldListNetworkNodes(context: TestContext) {
    val async = context.async()
    log.info("calling get: http://localhost:$port/api/rest/network/nodes")
    client.getFuture("/api/rest/network/nodes", headers = mapOf("Accept" to "application/json; charset=utf8"))
      .compose { it.body<List<SimpleNodeInfo>>() }
      .onSuccess { nodes ->
        context.assertThat(nodes.size, equalTo(3))
        val nodeInfoA = nodes.first { node -> node.legalIdentities.any { party -> party.name == bankA } }
        context.assertThat(nodeInfoA.addresses, hasItem(NetworkHostAndPort("localhost", nodeAPort)))
        val nodeInfoB = nodes.first { node -> node.legalIdentities.any { party -> party.name == bankB } }
        context.assertThat(nodeInfoB.addresses, hasItem(NetworkHostAndPort("localhost", nodeBPort)))
        val nodeInfoNotary = nodes.first { node -> node.legalIdentities.any { party -> party == driver!!.dsl.defaultNotaryIdentity } }
        context.assertThat(nodeInfoNotary.addresses, hasItem(notaryAddress))
      }
      .onSuccess { async.complete() }
      .catch(context::fail)
  }

  @Test
  fun shouldListNetworkNodesByHostAndPort(context: TestContext) {
    val async = context.async()
    log.info("calling get: https://localhost:$port/api/rest/network/nodes")
    client.getFuture("/api/rest/network/nodes",
      headers = mapOf("Accept" to "application/json; charset=utf8"),
      queryParameters = mapOf("host-and-port" to "localhost:${nodeBPort}"))
      .compose { it.body<JsonArray>() }
      .onSuccess { nodes ->
        val node = nodes.getJsonObject(0)
        val addresses = node.getJsonArray("addresses")
        context.assertThat(addresses.size(), equalTo(1))
        context.assertThat(
          addresses.getJsonObject(0).getString("host"),
          equalTo("localhost")
        )
        context.assertThat(
          addresses.getJsonObject(0).getInteger("port"),
          equalTo(nodeBPort)
        )
      }
      .onSuccess { async.complete() }
      .catch(context::fail)
  }

  @Test
  fun shouldGetPartyB(context: TestContext) {
    val cordaX500Name = CordaX500Name("PartyB", "New York", "US")
    val toString = cordaX500Name.toString()
    val encode = URLEncoder.encode(toString)
    assertThat(encode, `is`("O%3DPartyB%2C+L%3DNew+York%2C+C%3DUS"))
  }


  @Test
  fun shouldDecode(context: TestContext) {
    val encode = URLDecoder.decode("O%3DPartyB%2CL%3DNew+York%2CC%3DUS")
    val parse = CordaX500Name.parse(encode)
    val cordaX500Name = CordaX500Name("PartyB", "New York", "US")
    assertThat(parse, `is`(cordaX500Name))
  }

  @Test
  fun shouldListNetworkNodesByX509Name(context: TestContext) {
    val async = context.async()
    log.info("calling get: https://localhost:$port/api/rest/network/nodes")
    client.getFuture("/api/rest/network/nodes",
      queryParameters = mapOf("x500-name" to driver!!.dsl.defaultNotaryIdentity.name.toString()),
      headers = mapOf("Accept" to "application/json; charset=utf8"))
      .compose { it.body<JsonArray>() }
      .onSuccess { nodes ->
        assertThat(nodes.size(), equalTo(1))
        val node = nodes.getJsonObject(0)
        val addresses = node.getJsonArray("addresses")
        context.assertThat(addresses.size(), equalTo(1))
        context.assertThat(
          addresses.getJsonObject(0).getString("host"),
          equalTo("localhost")
        )
        context.assertThat(
          addresses.getJsonObject(0).getInteger("port"),
          equalTo(notaryAddress.port)
        )
      }
      .onSuccess { async.complete() }
      .catch(context::fail)
  }

  @Test
  fun `should return empty list if node not found`(context: TestContext) {
    val async = context.async()

    log.info("calling get: https://localhost:$port/api/rest/network/nodes")
    client.get(
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

    log.info("calling get: https://localhost:$port/api/rest/network/nodes/self")
    client.getFuture("/api/rest/network/nodes/self", headers = mapOf("Accept" to "application/json; charset=utf8"))
      .compose { it.body<JsonObject>() }
      .onSuccess { node ->
        val addresses = node.getJsonArray("addresses")
        context.assertThat(addresses.size(), equalTo(1))
        context.assertThat(
          addresses.getJsonObject(0).getString("host"),
          equalTo("localhost")
        )
        context.assertThat(
          addresses.getJsonObject(0).getInteger("port"),
          equalTo(nodeAPort)
        )
      }
      .onSuccess { async.complete() }
      .catch(context::fail)
  }

  @Test
  fun shouldListNetworkNotaries(context: TestContext) {
    val async = context.async()
    log.info("calling get: https://localhost:$port/api/rest/network/notaries")
    client.getFuture("/api/rest/network/notaries", headers = mapOf("Accept" to "application/json; charset=utf8"))
      .compose { it.body<List<Party>>() }
      .onSuccess { nodes ->
        context.assertThat(nodes.size, equalTo(1))
        context.assertThat(
          nodes.first(),
          equalTo(driver!!.dsl.defaultNotaryIdentity)
        )
      }
      .onSuccess { async.complete() }
      .catch(context::fail)
  }

  @Test
  fun shouldListFlows(context: TestContext) {
    val async = context.async()
    log.info("calling get: https://localhost:$port/api/rest/cordapps/flows")
    client.getFuture("/api/rest/cordapps/corda-core/flows")
      .compose { it.body<List<String>>() }
      .onSuccess { flows ->
        context.assertEquals(0, flows.size) // should not be exposing anything from corda's own flows
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
  fun `should Start a CashIssueFlow`(context: TestContext) {
    val async = context.async()

    getNotary()
      .compose { notary ->
        val json = JsonObject()
          .put("notary", notary)
          .put("amount", JsonObject(Json.encode(AMOUNT(10.00, Currency.getInstance("GBP")))))
          .put("issuerBankPartyRef", JsonObject().put("bytes", "AABBCC"))
        val path = "/api/rest/cordapps/corda-finance-workflows/flows/net.corda.finance.flows.CashIssueFlow"
        log.info("calling post: https://localhost:$port$path")
        val encodePrettily = json.encodePrettily()
        client.postFuture(path, mapOf("Accept" to "application/json; charset=utf8", "Content-length" to "${encodePrettily.length}"), body = encodePrettily)
      }
      .compose { it.body<JsonObject>() }
      .onSuccess { reply ->
        log.info("reply:" + reply.encodePrettily())
        context.assertThat(reply, notNullValue())
        context.assertThat(reply.getJsonObject("stx"), notNullValue())
        context.assertThat(reply.getJsonObject("recipient"), notNullValue())

        val signedTransactionJson = reply.getJsonObject("stx").encodePrettily()
        log.info(signedTransactionJson)

        //  todo round trip SignedTransaction
        // Failed to decode: Expected exactly 1 of {nodeSerializationEnv, driverSerializationEnv, contextSerializationEnv, inheritableContextSerializationEnv}
        withTestSerializationEnvIfNotSet {
          Json.decodeValue(signedTransactionJson, SignedTransaction::class.java)
        }
      }
      .onSuccess { async.complete() }
      .catch(context::fail)
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
      log.info("calling post: https://localhost:$port$path")

      client.postFuture(path,
        headers = mapOf("Accept" to "application/json; charset=utf8"),
        body = json
      )
        .compose { it.body<String>() }
        .onSuccess { error("should have failed") }
        .catch {
          assertTrue("should have failed", it is HttpStatusException)
          val cause = it as HttpStatusException
          assertEquals(Response.Status.BAD_REQUEST.statusCode, cause.statusCode, cause.message)
          context.assertThat(cause.payload, containsString("issuerBaaaaaankPartyRef"))
          async.complete()
        }
        .catch(context::fail)
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
      .catch(context::fail)
  }

  private fun getNotary(): Future<JsonObject> {
    return client.getFuture("/api/rest/network/notaries", headers = mapOf("Accept" to "application/json; charset=utf8"))
      .compose { it.body<JsonArray>() }
      .map { nodes ->
        //   val nodes = Json.decodeValue(it, object : TypeReference<List<Party>>() {})
        nodes.getJsonObject(0)
      }
  }


  @Test
  fun `should query the vault`(context: TestContext) {
    val async = context.async()
    log.info("calling get: https://localhost:${port}/api/rest/vault/vaultQuery")
    client.getFuture("/api/rest/vault/vaultQuery", headers = mapOf("Accept" to "application/json; charset=utf8"))
      .compose { it.body<JsonObject>() }
      .onSuccess { nodes ->
        vertxAssertThat(context, nodes, notNullValue())
      }
      .onSuccess { async.complete() }
      .catch(context::fail)
  }


  @Test
  fun `should query the vault for a specific type`(context: TestContext) {
    val async = context.async()

    log.info("calling get: https://localhost:${port}/api/rest/vault/vaultQuery?contract-state-type=" + ContractState::class.java.name)
    client.getFuture("/api/rest/vault/vaultQuery", headers = mapOf(
      "Accept" to "application/json; charset=utf8",
      "contract-state-type" to ContractState::class.java.name
    ))
      .compose { it.body<JsonObject>() }
      .onSuccess { nodes ->
        vertxAssertThat(context, nodes, notNullValue())
        async.complete()
      }
      .catch(context::fail)
  }


  @Test
  fun `should query the vault by type`(context: TestContext) {
    val async = context.async()
    val json = """
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
    log.info("calling post: https://localhost:${port}/api/rest/vault/vaultQueryBy")
    client.postFuture("/api/rest/vault/vaultQueryBy",
      body = json,
      headers = mapOf("Accept" to "application/json; charset=utf8"))
      .compose { it.body<JsonObject>() }
      .onSuccess { nodes ->
        vertxAssertThat(context, nodes, notNullValue())
        println(nodes.encodePrettily())
        async.complete()
      }
      .catch(context::fail)
  }


  @Test
  fun `should serialize various query`(context: TestContext) {
    val generalCriteria = VaultQueryCriteria(Vault.StateStatus.ALL)
    val currencyIndex = CashSchemaV1.PersistentCashState::currency.equal("GBP")
    val quantityIndex = CashSchemaV1.PersistentCashState::pennies.greaterThanOrEqual(0L)

    val customCriteria2 = VaultCustomQueryCriteria(quantityIndex)
    val customCriteria1 = VaultCustomQueryCriteria(currencyIndex)

    val criteria = generalCriteria
      .and(customCriteria1)
      .and(customCriteria2)

    val query = VaultQuery(criteria, contractStateType = Cash.State::class.java)

    val json = Json.encodePrettily(query)
    println(json)
  }


  @Test
  fun `should query the vault by various criteria`(context: TestContext) {
    val async = context.async()
    val json = """{
  "criteria" : {
    "@class" : ".QueryCriteria${'$'}AndComposition",
    "a" : {
      "@class" : ".QueryCriteria${'$'}AndComposition",
      "a" : {
        "@class" : ".QueryCriteria${'$'}VaultQueryCriteria",
        "status" : "ALL"
      },
      "b" : {
        "@class" : ".QueryCriteria${'$'}VaultCustomQueryCriteria",
        "expression" : {
          "@class" : ".CriteriaExpression${'$'}ColumnPredicateExpression",
          "column" : {
            "name" : "currency",
            "declaringClass" : "net.corda.finance.schemas.CashSchemaV1${'$'}PersistentCashState"
          },
          "predicate" : {
            "@class" : ".ColumnPredicate${'$'}EqualityComparison",
            "operator" : "EQUAL",
            "rightLiteral" : "GBP"
          }
        },
        "status" : "UNCONSUMED",
        "relevancyStatus" : "ALL"
      }
    },
    "b" : {
      "@class" : ".QueryCriteria${'$'}VaultCustomQueryCriteria",
      "expression" : {
        "@class" : ".CriteriaExpression${'$'}ColumnPredicateExpression",
        "column" : {
          "name" : "pennies",
          "declaringClass" : "net.corda.finance.schemas.CashSchemaV1${'$'}PersistentCashState"
        },
        "predicate" : {
          "@class" : ".ColumnPredicate${'$'}BinaryComparison",
          "operator" : "GREATER_THAN_OR_EQUAL",
          "rightLiteral" : 0
        }
      },
      "status" : "UNCONSUMED",
      "relevancyStatus" : "ALL"
    }
  },
  "contractStateType" : "net.corda.finance.contracts.asset.Cash${'$'}State"
}"""

    log.info("calling post: https://localhost:${port}/api/rest/vault/vaultQueryBy")
    client.postFuture("/api/rest/vault/vaultQueryBy", body = json, headers = mapOf("Accept" to "application/json; charset=utf8"))
      .compose { it.body<JsonObject>() }
      .onSuccess { nodes ->
        vertxAssertThat(context, nodes, notNullValue())
        async.complete()
      }
      .catch(context::fail)
  }


  @Test
  @Ignore
  fun `should issue obligation`(context: TestContext) {
    val async = context.async()
    getNotary().map {
      val notary = it
      val json = """
{
  "amount": {
    "quantity": 100,
    "displayTokenSize": 0.01,
    "token": "GBP"
  },
  "lender": {
    "name": "O=PartyB, L=New York, C=US",
    "owningKey": "GfHq2tTVk9z4eXgyWBgg9GY6LaCcQjjaSFVwKkJ5j1VyaU5nWjEijR28xxay"
  },
  "anonymous": false
}      """.trimIndent()

      val path = "/api/rest/cordapps/kotlin-source/flows/net.corda.examples.obligation.flows.IssueObligation\$Initiator"
      log.info("calling post: https://localhost:$port$path")

      client.postFuture(path, body = json, headers = mapOf("Accept" to "application/json; charset=utf8"))
        .compose { it.body<JsonObject>() }
        .onSuccess { reply ->
          log.info("reply:" + reply.encodePrettily())
          context.assertThat(reply, notNullValue())
          context.assertThat(reply.getJsonObject("stx"), notNullValue())
          context.assertThat(reply.getJsonObject("recipient"), notNullValue())
          async.complete()
        }
        .catch(context::fail)
    }
  }
}
