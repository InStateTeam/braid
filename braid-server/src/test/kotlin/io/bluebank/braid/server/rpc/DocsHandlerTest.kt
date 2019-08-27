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
import io.vertx.core.http.HttpMethod
import net.corda.core.flows.ContractUpgradeFlow
import net.corda.finance.flows.CashIssueFlow
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Ignore
import org.junit.Test


class DocsHandlerTest{
    @Test
    fun shouldDocumentDynamicClass() {
        val docsHandler = DocsHandler()
        val handler = FlowInitiator(mock()).getInitiator(CashIssueFlow::class)

        // need to be able to do this..
        val javaTypeIncludingSynthetics = handler.returnType.javaTypeIncludingSynthetics() as Class<*>
        assertThat("expecting java class",javaTypeIncludingSynthetics.name, CoreMatchers.equalTo("generated.net.corda.finance.flows.CashIssueFlowPayload"))
        //handler.returnType.javaType

        docsHandler.add("testGroup",false, HttpMethod.POST,"/test/path", handler)
    }

    @Test
    @Ignore
    fun shouldEscapeInnerClassDefinitionName() {
        val docsHandler = DocsHandler()
        val handler = FlowInitiator(mock()).getInitiator(ContractUpgradeFlow.Authorise::class)

        // need to be able to do this..
        val javaTypeIncludingSynthetics = handler.returnType.javaTypeIncludingSynthetics() as Class<*>
        assertThat("expecting java class",javaTypeIncludingSynthetics.name, CoreMatchers.equalTo("generated.net.corda.core.flows.ContractUpgradeFlow\$AuthorisePayload"))
        //handler.returnType.javaType

        docsHandler.add("testGroup",false, HttpMethod.POST,"/test/path", handler)
        val createSwagger = docsHandler.createSwagger()
        assertThat(createSwagger.definitions.get("ContractUpgradeFlow\$AuthorisePayload"), nullValue())
        assertThat(createSwagger.definitions.get("ContractUpgradeFlow.AuthorisePayload"), notNullValue())
    }
}