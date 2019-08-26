package io.bluebank.braid.server.rpc


import com.nhaarman.mockito_kotlin.mock
import io.bluebank.braid.corda.rest.docs.DocsHandler
import io.bluebank.braid.corda.rest.docs.javaTypeIncludingSynthetics
import io.vertx.core.http.HttpMethod
import net.corda.finance.flows.CashIssueFlow
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
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
}