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

import io.bluebank.braid.corda.server.Braid
import io.swagger.converter.ModelConverters
import net.corda.core.flows.ContractUpgradeFlow
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

class ModelConvertersTest {
  companion object {
    @BeforeClass
    @JvmStatic
    fun beforeClass() {
      Braid.init()
    }
  }

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

  @Test
  @Ignore // todo make ModelConverter for UpgradedContract
  fun testModelConvertersOfFlowConstrucor() {
    val constructors = ContractUpgradeFlow.Initiate::class.java.constructors
    constructors.forEach { constructor ->
      constructor.parameters.forEach { parameter ->
        val readAsProperty =
          ModelConverters.getInstance().readAsProperty(parameter.parameterizedType)
        assertThat(parameter.parameterizedType.toString(), readAsProperty, notNullValue())
      }
    }
  }

}