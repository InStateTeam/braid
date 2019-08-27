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


import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.server.flow.StartableByRPCFinder
import io.vertx.core.Future
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort

class RPCFactory(val userName:String,val password:String,val nodeAddress:String) {
    private val log = loggerFor<RPCFactory>()


    fun validConnection(): Future<CordaRPCOps> {
        log.info("Attempting to connect Braid RPC to:" + nodeAddress + " username:" + userName)
        val client = CordaRPCClient(NetworkHostAndPort.parse(nodeAddress))
        val connection = client.start(userName, password)
        val rpc = connection.proxy

//        while(!allFlowsRegistered(rpc)) {
//            Thread.sleep(100)      // todo move to vertx timer but difficult as this is all "config"
//        }

        return Future.succeededFuture(rpc)
    }

//    private fun allFlowsRegistered(rpc: CordaRPCOps): Boolean {
//        val registeredFlows = rpc.registeredFlows()
//
//        val findStartableByRPC = StartableByRPCFinder().findStartableByRPC()
//
//        log.info("registered: ${registeredFlows.size}  startable: ${findStartableByRPC.size}")
//        log.info("registered flows: ${registeredFlows}  startable flows: ${findStartableByRPC}")
//        return registeredFlows.size >= findStartableByRPC.size
//    }
}