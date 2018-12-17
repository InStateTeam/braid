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
package io.bluebank.braid.client.invocations.impl

import io.bluebank.braid.core.jsonrpc.JsonRPCCompletedResponse
import io.bluebank.braid.core.jsonrpc.JsonRPCErrorResponse
import io.bluebank.braid.core.jsonrpc.JsonRPCResultResponse
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.Future
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.jvm.javaMethod
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ObservableInvocationStrategyTest {
  companion object {
    private val log = loggerFor<FutureInvocationStrategyTest>()
  }

  @Test
  fun `that if Invocations provides the wrong requestId or payload an exception is raised`() {
    val invocations = MockInvocations {
      log.info("invoking", it)
      Future.succeededFuture()
    }

    val strategy = ObservableInvocationStrategy(invocations, TestInterface::testObservable.name, TestInterface::testObservable.javaMethod?.genericReturnType!!, arrayOf())
    val observable = strategy.getResult()
    assertEquals(0, invocations.activeRequestsCount, "that there are zero invocations so far")
    assertEquals(0, strategy.subscriberCount, "that the strategy has no subscribers")

    // kick off an invocation
    observable.subscribe { }
    assertEquals(1, invocations.activeRequestsCount, "that there is an invocation")
    assertEquals(1, strategy.subscriberCount, "that the strategy has no subscribers")
    assertEquals(strategy, invocations.getInvocationStrategy(1))

    assertFailsWith<IllegalStateException> { strategy.onNext(2, "hello") }
    assertFailsWith<IllegalStateException> { strategy.onError(2, RuntimeException("failed!")) }
    assertFailsWith<IllegalStateException> { strategy.onCompleted(2) }
    assertFailsWith<IllegalArgumentException> { strategy.onNext(1, null) }

    strategy.onNext(1, "hello")
    assertEquals(1, strategy.subscriberCount, "that the strategy has no subscribers")
    assertEquals(1, invocations.activeRequestsCount, "that there is an invocation")

    invocations.receive(JsonRPCCompletedResponse(id = 1))
    assertEquals(0, strategy.subscriberCount, "that the strategy has no subscribers")
    assertEquals(0, invocations.activeRequestsCount, "that there is an invocation")
  }

  @Test
  fun `that a faulty subscriber onNext doesn't cause the flow to stop`() {
    val invocations = MockInvocations {
      log.info("invoking", it)
      Future.succeededFuture()
    }

    val strategy = ObservableInvocationStrategy(invocations, TestInterface::testObservable.name, TestInterface::testObservable.javaMethod?.genericReturnType!!, arrayOf())
    val observable = strategy.getResult()
    assertEquals(0, invocations.activeRequestsCount, "that there are zero invocations so far")
    assertEquals(0, strategy.subscriberCount, "that the strategy has no subscribers")

    val counter = AtomicInteger(0)
    observable.subscribe({
      counter.incrementAndGet()
      throw Exception("fail on item")
    }, {
      counter.incrementAndGet()
    }, {
      counter.incrementAndGet()
    })

    // we should have a subscription now
    assertEquals(1, invocations.activeRequestsCount, "that there is an invocation")
    assertEquals(1, strategy.subscriberCount, "that there are zero invocations so far")
    assertEquals(0, counter.get(), "the subscriber should have been called yet")

    // now 'receive' a result
    invocations.receive(JsonRPCResultResponse(id = 1, result = "hello"))
    assertEquals(0, invocations.activeRequestsCount, "exception in subscriber should've cleared the subscription in the invocations map")
    assertEquals(0, strategy.subscriberCount, "exception should have cleared the subscription in the strategy's subscriptions map")
    assertEquals(2, counter.get(), "subscriber should have been called twice: once for onNext and once onError")

    // receiving a second result should have no effect because the subscription has been torn down
    invocations.receive(JsonRPCCompletedResponse(id = 1))
    assertEquals(0, invocations.activeRequestsCount, "that there is an invocation")
    assertEquals(0, strategy.subscriberCount, "that there are zero invocations so far")
    assertEquals(2, counter.get(), "subscriber should have been called twice: once for onNext and once onError")
  }

  @Test
  fun `that a faulty subscriber onNext and onError doesn't cause the flow to stop`() {
    val invocations = MockInvocations {
      log.info("invoking", it)
      Future.succeededFuture()
    }

    val strategy = ObservableInvocationStrategy(invocations, TestInterface::testObservable.name, TestInterface::testObservable.javaMethod?.genericReturnType!!, arrayOf())
    val observable = strategy.getResult()
    assertEquals(0, invocations.activeRequestsCount, "that there are zero invocations so far")
    assertEquals(0, strategy.subscriberCount, "that the strategy has no subscribers")

    val counter = AtomicInteger(0)
    observable.subscribe({
      counter.incrementAndGet()
      throw Exception("fail on item")
    }, {
      counter.incrementAndGet()
      throw Exception("fail on item")
    }, {
      counter.incrementAndGet()
    })

    // we should have a subscription now
    assertEquals(1, invocations.activeRequestsCount, "that there is an invocation")
    assertEquals(1, strategy.subscriberCount, "that there are zero invocations so far")
    assertEquals(0, counter.get(), "the subscriber should have been called yet")

    // now 'receive' a result
    invocations.receive(JsonRPCResultResponse(id = 1, result = "hello"))
    assertEquals(0, invocations.activeRequestsCount, "exception in subscriber should've cleared the subscription in the invocations map")
    assertEquals(0, strategy.subscriberCount, "exception should have cleared the subscription in the strategy's subscriptions map")
    assertEquals(2, counter.get(), "subscriber should have been called twice: once for onNext and once onError")

    // receiving a second result should have no effect because the subscription has been torn down
    invocations.receive(JsonRPCCompletedResponse(id = 1))
    assertEquals(0, invocations.activeRequestsCount, "that there is an invocation")
    assertEquals(0, strategy.subscriberCount, "that there are zero invocations so far")
    assertEquals(2, counter.get(), "subscriber should have been called twice: once for onNext and once onError")
  }

  @Test
  fun `that a faulty subscriber onError doesn't cause the flow to stop`() {
    val invocations = MockInvocations {
      log.info("invoking", it)
      Future.succeededFuture()
    }

    val strategy = ObservableInvocationStrategy(invocations, TestInterface::testObservable.name, TestInterface::testObservable.javaMethod?.genericReturnType!!, arrayOf())
    val observable = strategy.getResult()
    assertEquals(0, invocations.activeRequestsCount, "that there are zero invocations so far")
    assertEquals(0, strategy.subscriberCount, "that the strategy has no subscribers")

    val counter = AtomicInteger(0)
    observable.subscribe({
      counter.incrementAndGet()
    }, {
      counter.incrementAndGet()
      throw Exception("fail on item")
    }, {
      counter.incrementAndGet()
    })

    // we should have a subscription now
    assertEquals(1, invocations.activeRequestsCount, "that there is an invocation")
    assertEquals(1, strategy.subscriberCount, "that there are zero invocations so far")
    assertEquals(0, counter.get(), "the subscriber should have been called yet")

    // now 'receive' a result
    invocations.receive(JsonRPCResultResponse(id = 1, result = "result"))
    invocations.receive(JsonRPCErrorResponse.serverError(id = 1, message = "we send an error"))
    assertEquals(0, invocations.activeRequestsCount, "exception in subscriber should've cleared the subscription in the invocations map")
    assertEquals(0, strategy.subscriberCount, "exception should have cleared the subscription in the strategy's subscriptions map")
    assertEquals(2, counter.get(), "subscriber should have been called twice: once for onNext and once onError")

    // receiving a second result should have no effect because the subscription has been torn down
    invocations.receive(JsonRPCErrorResponse.internalError(id = 1, message = "failed"))
    assertEquals(0, invocations.activeRequestsCount, "that there is an invocation")
    assertEquals(0, strategy.subscriberCount, "that there are zero invocations so far")
    assertEquals(2, counter.get(), "subscriber should have been called twice: once for onNext and once onError")
  }
}