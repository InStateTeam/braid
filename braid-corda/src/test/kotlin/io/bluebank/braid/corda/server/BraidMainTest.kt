package io.bluebank.braid.corda.server

import io.bluebank.braid.core.async.catch
import io.bluebank.braid.core.async.onSuccess
import io.bluebank.braid.core.utils.JarDownloader
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import net.corda.core.identity.CordaX500Name
import net.corda.core.internal.toPath
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.User
import org.junit.runner.RunWith
import java.net.URL
import kotlin.test.AfterTest
import kotlin.test.Ignore
import kotlin.test.Test

@Ignore
@RunWith(VertxUnitRunner::class)
class BraidMainTest {
  companion object {
    private val jarFiles = listOf(
      "https://ci-artifactory.corda.r3cev.com/artifactory/corda-releases/net/corda/corda-finance-workflows/4.1/corda-finance-workflows-4.1.jar",
      "https://ci-artifactory.corda.r3cev.com/artifactory/corda-releases/net/corda/corda-finance-contracts/4.1/corda-finance-contracts-4.1.jar"
    )
    private val cordapps = JarDownloader().let { downloader ->
      jarFiles.map { downloader.uriToFile(URL(it)).toPath() }.map { TestCordappJar(jarFile = it) }
    }

    private val user = User("user1", "test", permissions = setOf("ALL"))
    private val bankA = CordaX500Name("PartyA", "London", "GB")
    private val bankB = CordaX500Name("PartyB", "New York", "US")
  }

  private lateinit var partyA: NodeHandle
  private lateinit var partyB: NodeHandle
  private lateinit var braidMain: BraidMain

  @AfterTest
  fun after(context: TestContext) {
    val async = context.async()
    braidMain.shutdown()
      .onSuccess {
        partyA.stop()
        partyB.stop()
      }
      .onSuccess { async.complete() }
      .catch { context.fail(it) }
  }

  @Test
  fun `test that we can invoke an issuance`(context: TestContext) {
    val async = context.async()
    driver(
      DriverParameters(
        cordappsForAllNodes = cordapps,
        waitForAllNodesToFinish = true,
        isDebug = true,
        startNodesInProcess = true
      )
    ) {
      val nodes = listOf(
        startNode(providedName = bankA, rpcUsers = listOf(user)),
        startNode(providedName = bankB, rpcUsers = listOf(user))
      ).map { it.getOrThrow() }
      partyA = nodes[0]
      partyB = nodes[1]

      braidMain = BraidMain(jarFiles, 3)
      braidMain.start(partyA.rpcAddress.toString(), "user1", "test", 8080)
        .compose {
          braidMain.start(partyB.rpcAddress.toString(), "user1", "test", 8081)
        }
    }.onSuccess { async.complete() }
      .catch { context.fail(it) }
  }

}


