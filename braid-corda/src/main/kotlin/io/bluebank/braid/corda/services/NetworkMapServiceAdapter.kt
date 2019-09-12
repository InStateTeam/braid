package io.bluebank.braid.corda.services

import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.DataFeed
import net.corda.core.node.NodeInfo
import net.corda.core.node.services.NetworkMapCache
import net.corda.core.utilities.NetworkHostAndPort

/**
 * interface for access to network map services
 */
interface NetworkMapServiceAdapter {

  fun networkMapSnapshot(): List<NodeInfo>
  fun wellKnownPartyFromX500Name(x500Name: CordaX500Name): Party?
  fun nodeInfoFromParty(party: AbstractParty): NodeInfo?
  fun notaryIdentities(): List<Party>
  fun nodeInfo(): NodeInfo
  fun notaryPartyFromX500Name(x500Name: CordaX500Name): Party?
  fun track(): DataFeed<List<NodeInfo>, NetworkMapCache.MapChange>
  fun getNodeByAddress(hostAndPort: NetworkHostAndPort): NodeInfo?
  fun getNodeByAddress(hostAndPort: String): NodeInfo? {
    return getNodeByAddress(NetworkHostAndPort.parse(hostAndPort))
  }

  fun getNodeByLegalName(name: CordaX500Name): NodeInfo?
}