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
package io.bluebank.braid.corda.rest.docs.v3

import io.bluebank.braid.corda.serialisation.BraidCordaJacksonInit
import io.swagger.v3.oas.models.media.StringSchema
import io.vertx.core.json.Json
import net.corda.core.contracts.TimeWindow
import net.corda.core.flows.WaitTimeUpdate
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Duration
import java.time.temporal.ChronoUnit

class ModelContextV3Test{
  @Before
  fun setUp() {
    BraidCordaJacksonInit.init()    // adds KotlinModule for required field parameter
  }

  @Test
  fun `should exclude availableComponentGroups from TraversableTransaction`() {

    val modelContext = ModelContextV3()
    modelContext.addType(TraversableTransaction::class.java)

    val wire = modelContext.models.get(TraversableTransaction::class.java.name)
    assertThat(wire,notNullValue())
    assertThat(wire?.properties?.get("availableComponentGroups"),nullValue())

  }

  @Test
  fun `should exclude availableComponentGroups from WireTransaction`() {

    val modelContext = ModelContextV3()
    modelContext.addType(WireTransaction::class.java)

    val wire = modelContext.models.get(WireTransaction::class.java.name)
    assertThat(wire,notNullValue())
    assertThat(wire?.properties?.get("availableComponentGroups"),nullValue())

  }

  @Test
  fun `should exclude length from TimeWindow`() {

    val modelContext = ModelContextV3()
    modelContext.addType(TimeWindow::class.java)

    val wire = modelContext.models.get(TimeWindow::class.java.name)
    assertThat(wire,notNullValue())
    assertThat(wire?.properties?.get("length"),nullValue())

  }

  @Test
  fun `that WaitTimeUpdate description is correct`() {
    val modelContext = ModelContextV3()
    modelContext.addType(WaitTimeUpdate::class.java)

    val waitTime = modelContext.models.get(WaitTimeUpdate::class.java.name)
    assertThat(waitTime,notNullValue())
    assertThat(waitTime?.properties?.get("waitTime")?.type, equalTo("string"))

  }

  @Test
  fun `that WaitTimeUpdate serializable is sensible`() {
    ModelContextV3()
    val expected = WaitTimeUpdate(Duration.of(10, ChronoUnit.DAYS))
    val encoded = Json.encode(expected)

    kotlin.test.assertEquals("{\"waitTime\":\"PT240H\"}", encoded)
  }
}