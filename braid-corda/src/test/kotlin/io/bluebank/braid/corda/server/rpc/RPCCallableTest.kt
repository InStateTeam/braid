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

import io.bluebank.braid.corda.server.BraidTestFlow
import net.corda.core.transactions.SignedTransaction
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import kotlin.reflect.jvm.javaType

class RPCCallableTest {
  @Test
  fun shouldBeCallableAndReturnTypeOfFlow() {

    val flow = BraidTestFlow::class

    val rpcCallable = RPCCallable(flow, flow.constructors.iterator().next())

    assertThat(
      rpcCallable.returnType.javaType.typeName,
      `is`(SignedTransaction::class.java.name)
    )
  }

}