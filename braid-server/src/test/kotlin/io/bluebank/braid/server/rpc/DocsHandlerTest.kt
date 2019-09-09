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
package io.bluebank.braid.server.rpc

import com.nhaarman.mockito_kotlin.mock
import io.bluebank.braid.corda.rest.docs.DocsHandler
import io.bluebank.braid.corda.rest.docs.javaTypeIncludingSynthetics
import io.bluebank.braid.core.synth.preferredConstructor
import io.vertx.core.http.HttpMethod
import net.corda.core.flows.ContractUpgradeFlow
import net.corda.finance.flows.CashIssueFlow
import net.corda.finance.flows.CashPaymentFlow
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.lang.reflect.Constructor

class DocsHandlerTest {
  @Test
  fun `should Document Dynamic Class`() {
    val docsHandler = DocsHandler()
    val handler = FlowInitiator(mock()).getInitiator(CashIssueFlow::class)

    // need to be able to do this..
    val javaTypeIncludingSynthetics =
        handler.returnType.javaTypeIncludingSynthetics() as Class<*>
    assertThat(
        "expecting java class",
        javaTypeIncludingSynthetics.name,
        CoreMatchers.equalTo("net.corda.finance.flows.AbstractCashFlow\$Result")
    )
    //handler.returnType.javaType

    docsHandler.add("testGroup", false, HttpMethod.POST, "/test/path", handler)
  }

  @Test
  //@Ignore
  fun `should Escape Inner Class Definition Name`() {
    val docsHandler = DocsHandler()
    val handler = FlowInitiator(mock()).getInitiator(ContractUpgradeFlow.Authorise::class)

    // need to be able to do this..
    val javaTypeIncludingSynthetics =
        handler.returnType.javaTypeIncludingSynthetics() as Class<*>
    assertThat(
        "expecting java class",
        javaTypeIncludingSynthetics.name,
        CoreMatchers.equalTo("java.lang.Void")
    )
    //handler.returnType.javaType

    docsHandler.add("testGroup", false, HttpMethod.POST, "/test/path", handler)
    val createSwagger = docsHandler.createSwagger()
    assertThat(
        createSwagger.definitions.get("ContractUpgradeFlow\$AuthorisePayload"),
        nullValue()
    )
    assertThat(
        createSwagger.definitions.get("ContractUpgradeFlow_AuthorisePayload"),
        notNullValue()
    )
  }


  @Test
  @Ignore
  fun `should Get Parameter Names for loaded CashPaymentFlowPayload`() {
    val preferredConstructor = CashPaymentFlow::class.java.preferredConstructor()

    val constructors = CashPaymentFlow::class.java.constructors
    val constructors2 = ContractUpgradeFlow.Authorise::class.java.constructors
             
    printParams(constructors)
    printParams(constructors2)

  }

  private fun printParams(constructors2: Array<Constructor<*>>) {
    for (constructor in constructors2) {
      println("$constructor - ")
      for (parameter in constructor.parameters) {
        //assertThat("expecting to find proper name",parameter.name, not(startsWith("arg")))
        print(" ${parameter.name},")
      }
      println()
    }
  }
}