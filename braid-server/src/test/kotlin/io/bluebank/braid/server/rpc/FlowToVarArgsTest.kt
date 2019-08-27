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

import net.corda.core.contracts.Amount
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.parsePublicKeyBase58
import net.corda.finance.AMOUNT
import net.corda.finance.flows.CashIssueFlow
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Test
import java.util.*

class FlowToVarArgsTest{
//    @Test
//    fun convertToVarArgs() {
//        val party = Party(CordaX500Name.parse("O=Notary Service, L=Zurich, C=CH"),
//                parsePublicKeyBase58("GfHq2tTVk9z4eXgyHW9wdnkysnj37Bq1wPe1WsrY4nDcvWJcjeCpxXHpDZwe"))
//       // val progressHandler = ops.startTrackedFlowDynamic(CashIssueFlow::class.java, )
//
//
//        val amount = Amount(100, Currency.getInstance("GBP"))
//        val cashIssueFlow = CashIssueFlow(amount, OpaqueBytes("123".toByteArray()), party)
//
//        val toVarArgs = cashIssueFlow.toVarArgs() as Array<Any>
//
//        assertThat(toVarArgs.size, equalTo(3))
//        assertThat(toVarArgs[0] as Amount<Currency>, `is`(amount))
//    }
}