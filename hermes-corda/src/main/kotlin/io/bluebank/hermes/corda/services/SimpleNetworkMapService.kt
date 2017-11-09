package io.bluebank.hermes.corda.services

import io.bluebank.hermes.corda.HermesConfig
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.NodeInfo
import net.corda.core.node.services.NetworkMapCache
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.node.services.api.ServiceHubInternal
import rx.Observable
import rx.Subscription

class SimpleNetworkMapService private constructor(private val networkMapCache: NetworkMapCache, config: HermesConfig) {
  constructor(services: ServiceHubInternal, config: HermesConfig) : this (services.networkMapCache, config)

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

  data class MapChange(val type: MapChangeType, val node: SimpleNodeInfo, val previousNode: SimpleNodeInfo? = null) {
   constructor(change: NetworkMapCache.MapChange) : this(
       when(change) {
         is NetworkMapCache.MapChange.Added -> MapChangeType.ADDED
         is NetworkMapCache.MapChange.Removed -> MapChangeType.REMOVED
         is NetworkMapCache.MapChange.Modified -> MapChangeType.MODIFIED
         else -> throw RuntimeException("unknown map change type ${change.javaClass}")
       },
       change.node.asSimple(),
       when(change) {
         is NetworkMapCache.MapChange.Modified -> change.previousNode.asSimple()
         else -> null
       }
   )
  }

  fun allNodes(): List<SimpleNodeInfo> {
    return networkMapCache.allNodes.map {
      it.asSimple()
    }
  }

  fun state() : Observable<Any> {
    return Observable.create { subscriber ->
      val dataFeed = networkMapCache.track()
      val snapshot = dataFeed.snapshot.map { SimpleNodeInfo(it) }

      subscriber.onNext(snapshot)
      var subscription : Subscription? = null

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

  fun notaryIdentities() : List<Party> {
    return networkMapCache.notaryIdentities
  }

  fun getNotary(cordaX500Name: CordaX500Name) : Party? {
    return networkMapCache.getNotary(cordaX500Name)
  }
}

private fun NetworkMapCache.MapChange.asSimple() : SimpleNetworkMapService.MapChange {
  return SimpleNetworkMapService.MapChange(this)
}

private fun NodeInfo.asSimple() : SimpleNetworkMapService.SimpleNodeInfo {
  return SimpleNetworkMapService.SimpleNodeInfo(this)
}
