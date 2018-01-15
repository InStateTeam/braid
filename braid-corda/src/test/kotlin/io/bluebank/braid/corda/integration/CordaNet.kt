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
    fun main(args: Array<String>) {
      CordaNet(5005, 8080).withCluster {
        waitForAllNodesToFinish()
      }
    }

    fun createCordaNet(): CordaNet {
      val startPort = findConsequtiveFreePorts(100)
      return CordaNet(startPort, startPort + 50)
    }
  }

  fun withCluster(callback: DriverDSLExposedInterface.() -> Unit) {
    println("Starting cluster with Corda base port $cordaStartingPort and Braid base port $braidStartingPort")
    driver(isDebug = true, startNodesInProcess = true, portAllocation = Incremental(cordaStartingPort)) {
      val parties = listOf("PartyA", "PartyB")

      // setup braid ports per party
      setupBraidPortsPerParty(parties, braidStartingPort)

      setCordappPackages("net.corda.finance", "io.bluebank.braid.corda.integration.cordapp")

      // start up the controller and all the parties
      val nodes = startupNodes(parties)

      // run the rest of the programme
      callback()
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

  private fun setupBraidPortsPerParty(parties: List<String>, braidStartingPort: Int) {
    with(System.getProperties()) {
      parties
          .mapIndexed { idx, party -> "braid.$party.port" to idx + braidStartingPort }
          .forEach { System.setProperty(it.first, it.second.toString()) }
    }
  }

}

