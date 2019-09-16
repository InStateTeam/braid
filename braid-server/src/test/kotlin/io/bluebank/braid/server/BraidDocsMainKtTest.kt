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
package io.bluebank.braid.server

import io.vertx.core.json.JsonObject
import org.junit.Assert.assertNotNull
import org.junit.Test

class BraidDocsMainKtTest {
  @Test
  fun `that we can generate a swagger definition V2`() {
    val jars = listOf(
      "https://repo1.maven.org/maven2/net/corda/corda-finance-contracts/4.0/corda-finance-contracts-4.0.jar",
      "https://repo1.maven.org/maven2/net/corda/corda-finance-workflows/4.0/corda-finance-workflows-4.0.jar"
    )
    val swagger = generateSwaggerText(2, jars)
    val json = JsonObject(swagger)
    assertNotNull(json.getJsonObject("paths")?.getJsonObject("/cordapps/corda-finance-workflows/flows/net.corda.finance.flows.CashExitFlow"))
  }

  @Test
  fun `that we can generate a swagger definition for V3`() {
    val jars = listOf(
      "https://repo1.maven.org/maven2/net/corda/corda-finance-contracts/4.0/corda-finance-contracts-4.0.jar",
      "https://repo1.maven.org/maven2/net/corda/corda-finance-workflows/4.0/corda-finance-workflows-4.0.jar"
    )
    val swagger = generateSwaggerText(3, jars)
    val json = JsonObject(swagger)
    assertNotNull(json.getJsonObject("paths")?.getJsonObject("/cordapps/corda-finance-workflows/flows/net.corda.finance.flows.CashExitFlow"))
  }
}