package io.bluebank.braid.server.rpc

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import net.corda.core.identity.Party
import net.corda.core.identity.PartyAndCertificate
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.NodeInfo
import net.corda.core.utilities.NetworkHostAndPort
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert
import org.junit.Assert.assertThat
import org.junit.Ignore
import org.junit.Test
import java.security.cert.CertPath
import java.security.cert.CertificateFactory
import java.util.Arrays.asList

@Ignore
class NetworkServiceTest{
    @Test
    fun shouldListSimpleNodeInfo() {
     //   val certFactory = CertificateFactory.getInstance("X509")
     //   val certPath = certFactory.generateCertPath(asList())

        val partyAndCertificate = mock<PartyAndCertificate> { }

        
        val addresses = asList(NetworkHostAndPort.parse("localhost:123"))
        val legalIdentitiesAndCerts = asList<PartyAndCertificate>(partyAndCertificate)
        val nodeInfo = NodeInfo(addresses, legalIdentitiesAndCerts, 1,2)
        val ops = mock<CordaRPCOps> {
            on { networkMapSnapshot() } doReturn asList(nodeInfo)
            on { nodeInfo() } doReturn nodeInfo
        }

        val network = NetworkService(ops)
        val simpleInfo = network.nodeInfo()
        assertThat(simpleInfo.addresses, `is`(addresses))
        assertThat(simpleInfo.legalIdentities, `is`(asList()))
    }
}