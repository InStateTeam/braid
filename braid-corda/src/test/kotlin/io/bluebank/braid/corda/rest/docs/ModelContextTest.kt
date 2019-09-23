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
package io.bluebank.braid.corda.rest.docs

import io.bluebank.braid.corda.serialisation.serializers.BraidCordaJacksonInit
import io.bluebank.braid.corda.swagger.CustomModelConverters
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
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