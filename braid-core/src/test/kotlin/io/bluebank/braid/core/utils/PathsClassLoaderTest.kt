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
package io.bluebank.braid.core.utils

import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.CoreMatchers.hasItem
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThat
import org.junit.Test
import java.io.File
import java.net.URL

class PathsClassLoaderTest {
  @Test
  fun `that we can load classes from web jars`() {
    val classLoader = PathsClassLoader.jarsClassLoader(
      "https://repo1.maven.org/maven2/net/corda/corda-finance-contracts/4.0/corda-finance-contracts-4.0.jar",
      "https://repo1.maven.org/maven2/net/corda/corda-finance-workflows/4.0/corda-finance-workflows-4.0.jar"
      )
    val clazz = classLoader.loadClass("net.corda.finance.flows.CashIssueFlow")
    assertNotNull(clazz)
  }

}
