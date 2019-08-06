package io.bluebank.braid.server

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import io.bluebank.braid.corda.serialisation.BraidCordaJacksonInit
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.json.Json
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.utilities.NetworkHostAndPort


class Braid

private val log = loggerFor<Braid>()


fun main(args: Array<String>) {

    if (args.size != 3) {
        throw IllegalArgumentException("Usage: Braid <node address> <username> <password>")
    }
    val nodeAddress = NetworkHostAndPort.parse(args[0])
    val username = args[1]
    val password = args[2]

    val client = CordaRPCClient(nodeAddress)
    val connection = client.start(username, password)
    val cordaRPCOperations = connection.proxy


    BraidServer(cordaRPCOperations).bootstrapBraid(8080)
            

    //connection.notifyServerAndClose()
}
