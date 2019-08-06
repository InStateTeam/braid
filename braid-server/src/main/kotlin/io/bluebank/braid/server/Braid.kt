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

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer
import com.typesafe.config.ConfigFactory
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.json.Json
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.utilities.NetworkHostAndPort


class Braid

private val log = loggerFor<Braid>()


fun main(args: Array<String>) {

    if (args.size != 4) {
        throw IllegalArgumentException("Usage: Braid <node address> <username> <password> <port>")
    }
    val nodeAddress = NetworkHostAndPort.parse(args[0])
    val username = args[1]
    val password = args[2]

    val client = CordaRPCClient(nodeAddress)
    val connection = client.start(username, password)
    val cordaRPCOperations = connection.proxy


    BraidServer(cordaRPCOperations).bootstrapBraid(Integer.valueOf(args[3]))
            

    //connection.notifyServerAndClose()
}
