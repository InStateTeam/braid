package io.bluebank.braid.corda.rest.docs

import io.bluebank.braid.corda.serialisation.BraidCordaJacksonInit
import io.bluebank.braid.corda.swagger.CustomModelConverters
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.testing.core.genericExpectEvents
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.*
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class ModelContextTest{
  @Before
  fun setUp() {
    BraidCordaJacksonInit.init()
    CustomModelConverters.init()
  }

  @Test
  @Ignore  //todo
  fun `should exclude availableComponentGroups from TraversableTransaction`() {

    val modelContext = ModelContext()
    modelContext.addType(TraversableTransaction::class.java)

    val wire = modelContext.models.get("TraversableTransaction")
    assertThat(wire,notNullValue())
    assertThat(wire?.properties?.get("availableComponentGroups"),nullValue())

  }

  @Test
  @Ignore  //todo
  fun `should exclude availableComponentGroups from WireTransaction`() {

    val modelContext = ModelContext()
    modelContext.addType(WireTransaction::class.java)

    val wire = modelContext.models.get("WireTransaction")
    assertThat(wire,notNullValue())
    assertThat(wire?.properties?.get("availableComponentHashes"),nullValue())
    assertThat(wire?.properties?.get("availableComponentHashes\$core"),nullValue())
    assertThat(wire?.properties?.get("availableComponentGroups"),nullValue())

  }
}