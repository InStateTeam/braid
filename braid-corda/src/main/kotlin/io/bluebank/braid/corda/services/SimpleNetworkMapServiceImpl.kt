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

import io.bluebank.braid.corda.BraidConfig
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.NodeInfo
import net.corda.core.node.services.NetworkMapCache
import net.corda.core.utilities.NetworkHostAndPort
import rx.Observable
import rx.Subscription

class SimpleNetworkMapServiceImpl(
  private val services: AppServiceHub,
  private val config: BraidConfig
) : SimpleNetworkMapService {

  data class SimpleNodeInfo(
    val addresses: List<NetworkHostAndPort>,
    val legalIdentities: List<Party>
  ) {

    // we map to work around the serialisation of
    constructor(nodeInfo: NodeInfo) : this(nodeInfo.addresses, nodeInfo.legalIdentities)
  }

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
      change.node.asSimple(),
      when (change) {
        is NetworkMapCache.MapChange.Modified -> change.previousNode.asSimple()
        else -> null
      }
    )
  }

  override fun myNodeInfo(): SimpleNodeInfo {
    return services.myInfo.asSimple()
  }

  override fun allNodes(): List<SimpleNodeInfo> {
    return services.networkMapCache.allNodes.map {
      it.asSimple()
    }
  }

  override fun state(): Observable<Any> {

    return Observable.create { subscriber ->
      val dataFeed = services.networkMapCache.track()
      services.transaction {
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
  }

  override fun notaryIdentities(): List<Party> {
    return services.transaction {
      services.networkMapCache.notaryIdentities
    }
  }

  override fun getNotary(cordaX500Name: CordaX500Name): Party? {
    return services.transaction {
      services.networkMapCache.getNotary(cordaX500Name)
    }
  }

  override fun getNodeByAddress(hostAndPort: String): SimpleNodeInfo? {
    return services.transaction {
      services.networkMapCache.getNodeByAddress(NetworkHostAndPort.parse(hostAndPort))
        ?.asSimple()
    }
  }

  override fun getNodeByLegalName(name: CordaX500Name): SimpleNodeInfo? {
    return services.transaction {
      services.networkMapCache.getNodeByLegalName(name)?.asSimple()
    }
  }
}

private fun NetworkMapCache.MapChange.asSimple(): SimpleNetworkMapServiceImpl.MapChange {
  return SimpleNetworkMapServiceImpl.MapChange(this)
}

private fun NodeInfo.asSimple(): SimpleNetworkMapServiceImpl.SimpleNodeInfo {
  return SimpleNetworkMapServiceImpl.SimpleNodeInfo(this)
}

fun <T> AppServiceHub.transaction(fn: () -> T): T {
  return fn()
}