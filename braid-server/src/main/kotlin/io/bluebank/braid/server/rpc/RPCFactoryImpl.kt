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
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort

interface RPCFactory {
  companion object {
    fun createRpcFactory(username: String, password: String, nodeAddress: NetworkHostAndPort): RPCFactory =
      RPCFactoryImpl(userName = username, password = password, nodeAddress = nodeAddress)

    fun createRpcFactoryStub(): RPCFactory = RPCFactoryStub()
  }

  fun validConnection(): CordaRPCOps
}

internal class RPCFactoryStub : RPCFactory {
  override fun validConnection(): CordaRPCOps {
    error("not implemented for stub")
  }
}

internal class RPCFactoryImpl(
  userName: String,
  password: String,
  nodeAddress: NetworkHostAndPort
) : RPCFactory {

  private val rpc: CordaRPCOps by lazy {
    log.info("Attempting to connect Braid RPC to:$nodeAddress username:$userName")
    val client = CordaRPCClient(nodeAddress)
    val connection = client.start(userName, password)
    connection.proxy
  }

  private val log = loggerFor<RPCFactoryImpl>()

  override fun validConnection(): CordaRPCOps {
    //  todo manage recover and reconnection etc probably with Future class
    return rpc
  }
}