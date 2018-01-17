/*
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

package io.bluebank.braid.corda.integration

import io.bluebank.braid.core.socket.findConsequtiveFreePorts
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.transactions.ValidatingNotaryService
import net.corda.nodeapi.internal.ServiceInfo
import net.corda.testing.driver.DriverDSLExposedInterface
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.PortAllocation.Incremental
import net.corda.testing.driver.driver
import net.corda.testing.setCordappPackages


class CordaNet(private val cordaStartingPort: Int = 5005, internal val braidStartingPort: Int = 8080) {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      CordaNet(5005, 8080).withCluster {
        readLine()
      }
    }

    fun createCordaNet(): CordaNet {
      val startPort = findConsequtiveFreePorts(100)
      return CordaNet(startPort, startPort + 50)
    }
  }

  fun withCluster(callback: DriverDSLExposedInterface.() -> Unit) {
    val parties = listOf("PartyA")

    // setup braid ports per party
    val systemProperties = setupBraidPortsPerParty(parties, braidStartingPort)

    println("Starting cluster with Corda base port $cordaStartingPort and Braid base port $braidStartingPort")
    driver(isDebug = false, startNodesInProcess = true, portAllocation = Incremental(cordaStartingPort), systemProperties = systemProperties) {

      setCordappPackages("net.corda.finance", "io.bluebank.braid.corda.integration.cordapp")

      // start up the controller and all the parties
      val nodeHandles = startupNodes(parties)

      // run the rest of the programme
      callback()

      nodeHandles.map {
        it.stop()
      }.forEach { it.getOrThrow() }
    }
  }

  private fun DriverDSLExposedInterface.startupNodes(parties: List<String>): List<NodeHandle> {
    return (
        listOf(startNode(
            providedName = CordaX500Name("Controller", "London", "GB"),
            advertisedServices = setOf(ServiceInfo(ValidatingNotaryService.type))))

            +
            parties.map { party ->
              startNode(providedName = CordaX500Name(party, "London", "GB"))
            }
        )
        .map { it.getOrThrow() }
  }

  private fun setupBraidPortsPerParty(parties: List<String>, braidStartingPort: Int): Map<String, String> {
    val result = with(System.getProperties()) {
      parties
          .mapIndexed { idx, party -> "braid.$party.port" to (idx + braidStartingPort).toString() }
          .toMap()
    }
    result.forEach {
      System.setProperty(it.key, it.value)
    }
    return result
  }
}

