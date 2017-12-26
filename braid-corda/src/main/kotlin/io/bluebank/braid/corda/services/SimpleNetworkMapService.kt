package io.bluebank.braid.corda.services

import io.bluebank.braid.corda.BraidConfig
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.NodeInfo
import net.corda.core.node.services.NetworkMapCache
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.node.services.api.ServiceHubInternal
import rx.Observable
import rx.Subscription

class SimpleNetworkMapService(private val services: ServiceHubInternal, private val config: BraidConfig) {

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

  fun myNodeInfo() : SimpleNodeInfo {
    return services.myInfo.asSimple()
  }

  fun allNodes(): List<SimpleNodeInfo> {
    return services.networkMapCache.allNodes.map {
      it.asSimple()
    }
  }

  fun state(): Observable<Any> {

    return Observable.create { subscriber ->
      val dataFeed = services.networkMapCache.track()
      services.database.transaction {
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

  fun notaryIdentities(): List<Party> {
    return services.database.transaction {
      services.networkMapCache.notaryIdentities
    }
  }

  fun getNotary(cordaX500Name: CordaX500Name): Party? {
    return services.database.transaction {
      services.networkMapCache.getNotary(cordaX500Name)
    }
  }

  fun getNodeByAddress(hostAndPort: String): SimpleNodeInfo? {
    return services.database.transaction {
      services.networkMapCache.getNodeByAddress(NetworkHostAndPort.parse(hostAndPort))?.asSimple()
    }
  }

  fun getNodeByLegalName(name: CordaX500Name): SimpleNodeInfo? {
    return services.database.transaction {
      services.networkMapCache.getNodeByLegalName(name)?.asSimple()
    }
  }
}

private fun NetworkMapCache.MapChange.asSimple(): SimpleNetworkMapService.MapChange {
  return SimpleNetworkMapService.MapChange(this)
}

private fun NodeInfo.asSimple(): SimpleNetworkMapService.SimpleNodeInfo {
  return SimpleNetworkMapService.SimpleNodeInfo(this)
}
