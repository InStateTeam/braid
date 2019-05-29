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
package com.template

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import net.corda.testing.node.User

/**
 * This file is exclusively for being able to run your nodes through an IDE (as opposed to using deployNodes)
 * Do not use in a production environment.
 *
 * To debug your CorDapp:
 *
 * 1. Run the "Run Template CorDapp" run configuration.
 * 2. Wait for all the nodes to start.
 * 3. Note the debug ports for each node, which should be output to the console. The "Debug CorDapp" configuration runs
 *    with port 5007, which should be "PartyA". In any case, double-check the console output to be sure.
 * 4. Set your breakpoints in your CorDapp code.
 * 5. Run the "Debug CorDapp" remote debug run configuration.
 */
fun main(args: Array<String>) {
  // No permissions required as we are not invoking flows.
  val user = User("user1", "test", permissions = setOf())
  driver(
    DriverParameters(
      isDebug = true,
      startNodesInProcess = true,
      waitForAllNodesToFinish = true,
      extraCordappPackagesToScan = listOf("com.template")
    )
  ) {
    val (partyA, partyB) = listOf(
      startNode(
        providedName = CordaX500Name("PartyA", "London", "GB"),
        rpcUsers = listOf(user)
      ),
      startNode(
        providedName = CordaX500Name("PartyB", "New York", "US"),
        rpcUsers = listOf(user)
      )
    ).map { it.getOrThrow() }

    startWebserver(partyA)
    startWebserver(partyB)
  }
}