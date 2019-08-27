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
import io.bluebank.braid.server.BraidTestFlow
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class RPCCallableTest {
    @Test
    fun shouldBeCallableWithOneParameter() {

        val flow = BraidTestFlow::class

        val rpcCallable = RPCCallable(mock(), flow.constructors.iterator().next())

        assertThat(rpcCallable.parameters.size,equalTo(3))
    }
}