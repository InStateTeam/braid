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