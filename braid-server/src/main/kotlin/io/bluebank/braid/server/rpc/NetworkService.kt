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

import io.bluebank.braid.server.domain.SimpleNodeInfo
import io.bluebank.braid.server.domain.toSimpleNodeInfo
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.NetworkHostAndPort
import java.util.stream.Collectors.toList
import javax.ws.rs.QueryParam

class NetworkService(val rpc: RPCFactory) {
  @ApiOperation(value = "Retrieves all nodes if neither query parameter is supplied. Otherwise returns a list of one node matching the supplied query parameter.")
  fun nodeInfo(): SimpleNodeInfo {
    return rpc.validConnection().nodeInfo().toSimpleNodeInfo()
  }

  @ApiOperation(value = "Retrieves all nodes if neither query parameter is supplied. Otherwise returns a list of one node matching the supplied query parameter.")
  fun nodes(
    @ApiParam(
      value = "[host]:[port] for the Corda P2P of the node",
      example = "localhost:10000"
    ) @QueryParam(value = "host-and-port") hostAndPort: String? = null,
    @ApiParam(
      value = "the X500 name for the node",
      example = "O=PartyB, L=New York, C=US"
    ) @QueryParam(value = "x500-name") x500Name: String? = null
  ): List<SimpleNodeInfo> {
    return when {
      hostAndPort?.isNotEmpty() ?: false -> {
        val address = NetworkHostAndPort.parse(hostAndPort!!)
        rpc.validConnection().networkMapSnapshot().stream()
          .filter { node -> node.addresses.contains(address) }
          .map { node -> node.toSimpleNodeInfo() }
          .collect(toList())
      }
      x500Name?.isNotEmpty() ?: false -> {
        val x500Name1 = CordaX500Name.parse(x500Name!!)
        val party = rpc.validConnection().wellKnownPartyFromX500Name(x500Name1)
        listOfNotNull(rpc.validConnection().nodeInfoFromParty(party!!)?.toSimpleNodeInfo())
      }
      else -> rpc.validConnection().networkMapSnapshot().stream().map { node -> node.toSimpleNodeInfo() }.collect(
        toList()
      )
    }
  }

  // example http://localhost:8080/api/rest/network/notaries?x500-name=O%3DNotary%20Service,%20L%3DZurich,%20C%3DCH
  fun notaries(
    @ApiParam(
      value = "the X500 name for the node",
      example = "O=PartyB, L=New York, C=US"
    ) @QueryParam(value = "x500-name") x500Name: String? = null
  ): List<Party> {
    return when {
      x500Name?.isNotEmpty() ?: false -> listOfNotNull(
        rpc.validConnection().notaryPartyFromX500Name(
          CordaX500Name.parse(x500Name!!)
        )
      )
      else -> rpc.validConnection().notaryIdentities()
    }
  }
}