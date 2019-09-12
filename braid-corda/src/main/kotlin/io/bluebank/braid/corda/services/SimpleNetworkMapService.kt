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
package io.bluebank.braid.corda.services

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.NodeInfo
import net.corda.core.node.services.NetworkMapCache
import net.corda.core.utilities.NetworkHostAndPort
import rx.Observable
import rx.Subscription
import java.util.stream.Collectors
import javax.ws.rs.QueryParam

data class SimpleNodeInfo(
  val addresses: List<NetworkHostAndPort>,
  val legalIdentities: List<Party>
) {

  // we map to work around the serialisation of
  constructor(nodeInfo: NodeInfo) : this(nodeInfo.addresses, nodeInfo.legalIdentities)
}

fun NodeInfo.toSimpleNodeInfo(): SimpleNodeInfo {
  return SimpleNodeInfo(this.addresses, this.legalIdentities)
}

class SimpleNetworkMapService(
  private val networkMapServiceAdapter: NetworkMapServiceAdapter
) {

  enum class MapChangeType {
    ADDED,
    REMOVED,
    MODIFIED
  }

  data class MapChange(
    val type: MapChangeType,
    val node: SimpleNodeInfo,
    val previousNode: SimpleNodeInfo? = null
  ) {

    constructor(change: NetworkMapCache.MapChange) : this(
      when (change) {
        is NetworkMapCache.MapChange.Added -> MapChangeType.ADDED
        is NetworkMapCache.MapChange.Removed -> MapChangeType.REMOVED
        is NetworkMapCache.MapChange.Modified -> MapChangeType.MODIFIED
        else -> throw RuntimeException("unknown map change type ${change.javaClass}")
      },
      change.node.toSimpleNodeInfo(),
      when (change) {
        is NetworkMapCache.MapChange.Modified -> change.previousNode.toSimpleNodeInfo()
        else -> null
      }
    )
  }

  @ApiOperation(value = "Retrieves all nodes if neither query parameter is supplied. Otherwise returns a list of one node matching the supplied query parameter.")
  fun myNodeInfo(): SimpleNodeInfo {
    return networkMapServiceAdapter.nodeInfo().toSimpleNodeInfo()
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
        networkMapServiceAdapter.networkMapSnapshot().stream()
          .filter { node -> node.addresses.contains(address) }
          .map { node -> node.toSimpleNodeInfo() }
          .collect(Collectors.toList())
      }
      x500Name?.isNotEmpty() ?: false -> {
        val x500Name1 = CordaX500Name.parse(x500Name!!)
        val party = networkMapServiceAdapter.wellKnownPartyFromX500Name(x500Name1)
        listOfNotNull(networkMapServiceAdapter.nodeInfoFromParty(party!!)?.toSimpleNodeInfo())
      }
      else -> networkMapServiceAdapter.networkMapSnapshot().stream().map { node -> node.toSimpleNodeInfo() }.collect(
        Collectors.toList()
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
        networkMapServiceAdapter.notaryPartyFromX500Name(
          CordaX500Name.parse(x500Name!!)
        )
      )
      else -> networkMapServiceAdapter.notaryIdentities()
    }
  }

  fun allNodes(): List<SimpleNodeInfo> {
    return networkMapServiceAdapter.networkMapSnapshot().map {
      it.toSimpleNodeInfo()
    }
  }

  fun state(): Observable<Any> {
    return Observable.create { subscriber ->
      val dataFeed = networkMapServiceAdapter.track()
      val snapshot = dataFeed.snapshot.map { SimpleNodeInfo(it) }
      subscriber.onNext(snapshot)
      var subscription: Subscription? = null

      subscription = dataFeed.updates.subscribe { change ->
        if (subscriber.isUnsubscribed) {
          subscription?.unsubscribe()
          subscription = null
        } else {
          subscriber.onNext(change.asSimple())
        }
      }
    }
  }

  fun notaryIdentities(): List<Party> {
    return networkMapServiceAdapter.notaryIdentities()
  }

  fun getNotary(cordaX500Name: CordaX500Name): Party? {
    return networkMapServiceAdapter.notaryPartyFromX500Name(cordaX500Name)
  }

  fun getNodeByAddress(hostAndPort: String): SimpleNodeInfo? {
    return networkMapServiceAdapter.getNodeByAddress(hostAndPort)?.toSimpleNodeInfo()
  }

  fun getNodeByLegalName(name: CordaX500Name): SimpleNodeInfo? {
    return networkMapServiceAdapter.getNodeByLegalName(name)?.toSimpleNodeInfo()
  }
}

private fun NetworkMapCache.MapChange.asSimple(): SimpleNetworkMapService.MapChange {
  return SimpleNetworkMapService.MapChange(this)
}

fun <T> AppServiceHub.transaction(fn: () -> T): T {
  return fn()
}