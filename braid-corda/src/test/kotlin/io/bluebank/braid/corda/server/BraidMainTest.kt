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
import net.corda.testing.driver.driver
import net.corda.testing.node.User
import org.junit.runner.RunWith
import java.net.URL
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

  @Test
  fun `test that we can invoke an issuance`(context: TestContext) {
    val async = context.async()
    driver(
      DriverParameters(
        cordappsForAllNodes = cordapps,
        waitForAllNodesToFinish = false,
        isDebug = true,
        startNodesInProcess = true
      )
    ) {
      val (partyA, partyB) = listOf(
        startNode(providedName = bankA, rpcUsers = listOf(user)),
        startNode(providedName = bankB, rpcUsers = listOf(user))
      ).map { it.getOrThrow() }

      val braidMain = BraidMain(jarFiles, 3)

      braidMain
        .start(partyA.rpcAddress.toString(), "user1", "test", 8080)
        .compose {
          braidMain.start(partyB.rpcAddress.toString(), "user1", "test", 8081)
        }
        .compose {
          braidMain.shutdown()
        }
    }
      .onSuccess { async.complete() }
      .catch { context.fail(it) }
  }

}


