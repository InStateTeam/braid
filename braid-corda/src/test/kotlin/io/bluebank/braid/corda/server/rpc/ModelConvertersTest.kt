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
package io.bluebank.braid.corda.server.rpc

import io.swagger.converter.ModelConverters
import io.swagger.models.properties.ArrayProperty
import io.swagger.models.properties.StringProperty
import net.corda.core.flows.ContractUpgradeFlow
import net.corda.core.node.services.vault.QueryCriteria
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore
import org.junit.Test

class ModelConvertersTest {


  @Test
  fun testModelConverters() {
    val java = net.corda.core.contracts.UpgradedContract::class.java
    val readAsProperty = ModelConverters.getInstance().readAsProperty(java)
    println(readAsProperty)

  }

  @Test
  fun testModelConvertersOfFlow() {
    val java = ContractUpgradeFlow.Initiate::class.java
    val readAsProperty = ModelConverters.getInstance().readAsProperty(java)
    println(readAsProperty)
  }


  // swagger 3 error for Semantic error at definitions.FungibleAssetQueryCriteria.properties.contractStateTypes
  @Test
  fun testModelFungibleAssetQueryCriteria() {
    val java = QueryCriteria.FungibleAssetQueryCriteria::class.java
    val read = ModelConverters.getInstance().readAll(java)
    val model = read.get("FungibleAssetQueryCriteria")
    val types = model?.properties?.get("contractStateTypes") as ArrayProperty
    assertThat(types.items is StringProperty, equalTo(true))
  }

  @Test
  @Ignore // todo make ModelConverter for UpgradedContract
  fun testModelConvertersOfFlowConstrucor() {
    val constructors = ContractUpgradeFlow.Initiate::class.java.constructors

    constructors.forEach {
      it.parameters.forEach {
        val readAsProperty =
          ModelConverters.getInstance().readAsProperty(it.parameterizedType)
        assertThat(it.parameterizedType.toString(), readAsProperty, notNullValue())
      }
    }
  }
}