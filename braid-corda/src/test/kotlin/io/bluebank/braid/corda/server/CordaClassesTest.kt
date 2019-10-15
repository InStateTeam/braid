package io.bluebank.braid.corda.server

import io.bluebank.braid.corda.server.CordaClassesTest.Companion.classes
import io.bluebank.braid.core.utils.toJarsClassLoader
import io.bluebank.braid.core.utils.tryWithClassLoader
import io.github.classgraph.ClassGraph
import net.corda.core.contracts.ContractState
import net.corda.finance.contracts.asset.Cash
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass

class CordaClassesTest {
  companion object {
    val jars = listOf(
        "https://ci-artifactory.corda.r3cev.com/artifactory/corda-releases/net/corda/corda-finance-workflows/4.1/corda-finance-workflows-4.1.jar",
        "https://ci-artifactory.corda.r3cev.com/artifactory/corda-releases/net/corda/corda-finance-contracts/4.1/corda-finance-contracts-4.1.jar"
    )

    val classes =
      tryWithClassLoader(jars.toJarsClassLoader()){
        CordaClasses().readCordaClasses()
      }
    }

  
  @Test
  fun `should have cash state`() {
    assertThat(classes, hasItem(Cash.State::class))
  }


  @Test
  fun `should match cash state`() {
    val classInfo = ClassGraph().whitelistClasses(Cash.State::class.java.name)
        .enableAnnotationInfo()
        .scan()
        .allClasses[0]
    assertThat(CordaClasses().isCordaSerializedClass(classInfo), equalTo(true))
  }


}