package io.bluebank.braid.corda.server

import io.bluebank.braid.core.utils.toJarsClassLoader
import io.bluebank.braid.core.utils.tryWithClassLoader
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.OpenAPI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BraidDocsMainTest {
  companion object {
    val jars = listOf(
      "https://ci-artifactory.corda.r3cev.com/artifactory/corda-releases/net/corda/corda-finance-workflows/4.1/corda-finance-workflows-4.1.jar",
      "https://ci-artifactory.corda.r3cev.com/artifactory/corda-releases/net/corda/corda-finance-contracts/4.1/corda-finance-contracts-4.1.jar"
    )
  }

  @Test
  fun `that we can generate swagger json`() {
    val swagger = tryWithClassLoader(jars.toJarsClassLoader()) {
      BraidDocsMain().swaggerText(3)
    }
    val openApi = Json.mapper().readValue(swagger, OpenAPI::class.java)
    assertEquals(1, openApi.servers.size)
    assertEquals("http://localhost:8080/api/rest", openApi.servers[0].url)
    assertTrue(openApi.paths.containsKey("/network/nodes"))
    assertTrue(openApi.paths.containsKey("/cordapps"))
    assertTrue(openApi.paths.containsKey("/cordapps/corda-finance-workflows/flows/net.corda.finance.flows.CashPaymentFlow"))
    assertTrue(openApi.components.schemas.containsKey("Error"))
    assertTrue(openApi.components.schemas.containsKey("net.corda.finance.contracts.Commodity"))
  }
}