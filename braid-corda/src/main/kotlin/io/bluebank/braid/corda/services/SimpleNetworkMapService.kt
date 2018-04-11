package io.bluebank.braid.corda.services

import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import rx.Observable

interface SimpleNetworkMapService {
    fun myNodeInfo() : SimpleNetworkMapServiceImpl.SimpleNodeInfo
    fun allNodes(): List<SimpleNetworkMapServiceImpl.SimpleNodeInfo>
    fun state(): Observable<Any>
    fun notaryIdentities(): List<Party>
    fun getNotary(cordaX500Name: CordaX500Name): Party?
    fun getNodeByAddress(hostAndPort: String): SimpleNetworkMapServiceImpl.SimpleNodeInfo?
    fun getNodeByLegalName(name: CordaX500Name): SimpleNetworkMapServiceImpl.SimpleNodeInfo?
}