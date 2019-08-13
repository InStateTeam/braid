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
package io.bluebank.braid.server.rpc

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import io.bluebank.braid.corda.serialisation.BraidCordaJacksonInit
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.json.Json
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort

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
    val ops = connection.proxy


    BraidCordaJacksonInit.init()

    val it = SimpleModule()
            .addSerializer(rx.Observable::class.java, ToStringSerializer())
           
    Json.mapper.registerModule(it)
    Json.prettyMapper.registerModule(it)


    log.info("currentNodeTime"+ Json.encodePrettily( ops.currentNodeTime()))
    log.info("nodeInfo" + Json.encodePrettily(ops.nodeInfo()))
    log.info("nodeInfo/addresses" + Json.encodePrettily(ops.nodeInfo().addresses))
    log.info("nodeInfo/legalIdentities" + Json.encodePrettily(ops.nodeInfo().legalIdentities))
  //  log.info(cordaRPCOperations.nodeInfoFromParty(Party(CordaX500Name.parse(""), PublicKey())).toString())
    log.info("notaryIdentities:" + Json.encodePrettily(ops.notaryIdentities())  )
    log.info("networkMapFeed:" + Json.encodePrettily(ops.networkMapFeed())    )
    log.info("registeredFlows:" + Json.encodePrettily(ops.registeredFlows()))
    //cordaRPCOperations.


    //ops.
    //ops.startFlow()
    connection.notifyServerAndClose()
}


