package io.bluebank.braid.server.domain

import net.corda.core.identity.Party
import net.corda.core.node.NodeInfo
import net.corda.core.utilities.NetworkHostAndPort

data class SimpleNodeInfo(
        val addresses: List<NetworkHostAndPort>,
        val legalIdentities: List<Party>
)

fun NodeInfo.toSimpleNodeInfo(): SimpleNodeInfo{
   return SimpleNodeInfo(this.addresses,this.legalIdentities)
}