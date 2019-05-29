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
package io.bluebank.braid.corda.integration

import io.bluebank.braid.core.logging.LogInitialiser
import io.bluebank.braid.core.socket.findConsecutiveFreePorts
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.*
import java.net.ServerSocket

class DynamicPortAllocation : PortAllocation() {
  override fun nextPort(): Int {
    return ServerSocket(0).use {
      it.localPort
    }
  }
}

class CordaNet(
  private val cordaStartingPort: Int = 5005,
  internal val braidStartingPort: Int = 8080
) {

  companion object {
    init {
      LogInitialiser.init()
    }

    @JvmStatic
    fun main(args: Array<String>) {
      CordaNet(5005, 8080).withCluster {
        readLine()
      }
    }

    fun createCordaNet(): CordaNet {
      val startPort = findConsecutiveFreePorts(100)
      return CordaNet(startPort, startPort + 50)
    }
  }

  fun withCluster(callback: DriverDSL.() -> Unit) {
    val parties = listOf("PartyA")
    val portAllocation = DynamicPortAllocation()

    // setup braid ports per party
    val systemProperties = setupBraidPortsPerParty(parties, braidStartingPort)

    driver(
      DriverParameters(
        isDebug = false,
        startNodesInProcess = true,
        portAllocation = portAllocation,
        systemProperties = systemProperties,
        extraCordappPackagesToScan = listOf(
          "net.corda.finance",
          "io.bluebank.braid.corda.integration.cordapp"
        )
      )
    ) {
      // start up the controller and all the parties
      val nodeHandles = startupNodes(parties)

      // run the rest of the programme
      callback()

      nodeHandles.map { it.stop() }
    }
  }

  fun braidPortForParty(party: String) : Int {
    return System.getProperty("braid.$party.port")?.toInt() ?: error("could not locate braid port for $party")
  }

  private fun DriverDSL.startupNodes(parties: List<String>): List<NodeHandle> {
    // start the nodes sequentially, to minimise port clashes
    return parties.map { party ->
      startNode(providedName = CordaX500Name(party, "London", "GB")).getOrThrow()
    }
  }

  private fun setupBraidPortsPerParty(
    parties: List<String>,
    braidStartingPort: Int
  ): Map<String, String> {
    return parties
      .mapIndexed { idx, party -> "braid.$party.port" to (idx + braidStartingPort).toString() }
      .toMap()
      .apply {
        println("braid port map")
        forEach {
          println("${it.key} = ${it.value}")
          System.setProperty(it.key, it.value)
        }
      }
  }
}

