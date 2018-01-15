package io.bluebank.braid.corda.integration

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.transactions.ValidatingNotaryService
import net.corda.nodeapi.User
import net.corda.nodeapi.internal.ServiceInfo
import net.corda.testing.driver.DriverDSLExposedInterface
import net.corda.testing.driver.driver
import net.corda.testing.setCordappPackages


fun main(args: Array<String>) {
  createCluster {
    this.waitForAllNodesToFinish()
  }
}

fun createCluster(callback: DriverDSLExposedInterface.() -> Unit) {
  val user = User("user1", "test", permissions = setOf())
  driver(isDebug = true, startNodesInProcess = true) {
    setCordappPackages("net.corda.finance", "io.bluebank.braid.corda.integration.cordapp")
    val (controller, nodeA, nodeB) = listOf(
        startNode(
            providedName = CordaX500Name("Controller", "London", "GB"),
            advertisedServices = setOf(ServiceInfo(ValidatingNotaryService.type))),
        startNode(providedName = CordaX500Name("PartyA", "London", "GB")),
        startNode(providedName = CordaX500Name("PartyB", "New York", "US"))).map { it.getOrThrow() }
    callback()
  }
}
