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

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import net.corda.core.identity.PartyAndCertificate
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.NodeInfo
import net.corda.core.utilities.NetworkHostAndPort
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Ignore
import org.junit.Test
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