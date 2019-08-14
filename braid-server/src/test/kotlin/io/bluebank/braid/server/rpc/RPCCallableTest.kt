package io.bluebank.braid.server.rpc

import io.bluebank.braid.server.BraidTestFlow
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class RPCCallableTest {
    @Test
    fun shouldBeCallableWithOneParameter() {

        val flow = BraidTestFlow::class
        val rpcCallable = RPCCallable(flow.constructors.iterator().next())

        assertThat(rpcCallable.parameters.size,equalTo(3))
    }
}