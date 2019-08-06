package io.bluebank.braid.server

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import io.bluebank.braid.corda.BraidConfig
import io.bluebank.braid.corda.rest.RestConfig
import io.bluebank.braid.corda.serialisation.AmountDeserializer
import io.bluebank.braid.corda.serialisation.AmountSerializer
import io.bluebank.braid.corda.serialisation.BraidCordaJacksonInit
import io.bluebank.braid.corda.services.SimpleNetworkMapServiceImpl
import io.bluebank.braid.core.logging.loggerFor
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.vertx.core.AsyncResult
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.NetworkHostAndPort
import java.security.PublicKey
import javax.ws.rs.QueryParam

class RPCTest

private val log = loggerFor<RPCTest>()

fun main(args: Array<String>) {

    if (args.size != 3) {
        throw IllegalArgumentException("Usage: RPCTest <node address> <username> <password>")
    }
    val nodeAddress = NetworkHostAndPort.parse(args[0])
    val username = args[1]
    val password = args[2]

    val client = CordaRPCClient(nodeAddress)
    val connection = client.start(username, password)
    val cordaRPCOperations = connection.proxy


    BraidCordaJacksonInit.init()

    val it = SimpleModule()
            .addSerializer(rx.Observable::class.java, ToStringSerializer())
           
    Json.mapper.registerModule(it)
    Json.prettyMapper.registerModule(it)


    log.info("currentNodeTime"+ Json.encodePrettily( cordaRPCOperations.currentNodeTime()))
    log.info("nodeInfo" + Json.encodePrettily(cordaRPCOperations.nodeInfo()))
    log.info("nodeInfo/addresses" + Json.encodePrettily(cordaRPCOperations.nodeInfo().addresses))
    log.info("nodeInfo/legalIdentities" + Json.encodePrettily(cordaRPCOperations.nodeInfo().legalIdentities))
  //  log.info(cordaRPCOperations.nodeInfoFromParty(Party(CordaX500Name.parse(""), PublicKey())).toString())
    log.info("notaryIdentities:" + Json.encodePrettily(cordaRPCOperations.notaryIdentities())  )
    log.info("networkMapFeed:" + Json.encodePrettily(cordaRPCOperations.networkMapFeed())    )
    log.info("registeredFlows:" + Json.encodePrettily(cordaRPCOperations.registeredFlows()))
    //cordaRPCOperations.

    connection.notifyServerAndClose()
}


