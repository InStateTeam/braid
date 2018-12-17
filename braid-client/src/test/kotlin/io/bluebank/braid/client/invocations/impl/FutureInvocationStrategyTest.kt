package io.bluebank.braid.client.invocations.impl

import io.bluebank.braid.core.jsonrpc.JsonRPCErrorResponse
import io.bluebank.braid.core.jsonrpc.JsonRPCResultResponse
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.Future
import org.junit.Test
import kotlin.reflect.jvm.javaMethod
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FutureInvocationStrategyTest {
  companion object {
    private val log = loggerFor<FutureInvocationStrategyTest>()
  }

  @Test
  fun `that if Invocations provides the wrong requestId an exception is raised`() {
    var requestId: Long = -1

    val invocations = MockInvocations {
      requestId = it.id
      log.info("invoking", it)
      Future.succeededFuture()
    }

    val strategy = FutureInvocationStrategy(invocations, TestInterface::testFuture.name, TestInterface::testFuture.javaMethod?.genericReturnType!!, arrayOf())
    val future = strategy.getResult() // kicks off the invocation
    assertEquals(1, invocations.activeRequestsCount, "that there is one active request")
    assertEquals(strategy, invocations.getInvocationStrategy(requestId), "that the strategy is our future strategy")
    assertFalse(future.isComplete, "that the future should not have completed")

    // simulate receiving a message that the invocation has never been
    invocations.receive(JsonRPCResultResponse(id = 2, result = "dodgy result"))
    assertEquals(1, invocations.activeRequestsCount, "that there is still one active request")
    assertEquals(strategy, invocations.getInvocationStrategy(requestId), "that the strategy is our future strategy")
    assertFalse(future.isComplete, "that the future should not have completed")

    // now let's assume that InvocationsImpl has broken - does the future strategy deal with dodgy requestId
    // send in a response but on a different requestId - this should throw an exception
    assertFailsWith<IllegalStateException> { strategy.onNext(2, "this should throw because of the requestId not matching") }
    assertFailsWith<IllegalStateException> { strategy.onError(2, Exception("this should also throw because of the requestId not matching")) }
    assertFailsWith<IllegalStateException> { strategy.onCompleted(2) }

    // now try completing the future
    invocations.receive(JsonRPCResultResponse(id = 1, result = "pi"))
    assertEquals(0, invocations.activeRequestsCount, "that the invocation has been completed")
    assertTrue(future.isComplete, "that the future should not have completed")
    assertEquals("pi", future.result())

    // now let's imagine we get messages for a completed future
    assertFailsWith<IllegalStateException> { strategy.onNext(1, "this should throw because future is already completed") }
    assertFailsWith<IllegalStateException> { strategy.onError(1, Exception("this throws because future is already completed")) }
    // we should be able to call the onCompleted message for completeness - this shouldn't be called ever by the protocol
    // never the less, we check that the [InvocationStrategy] internal API is consistent
    strategy.onCompleted(1)

    // but a second time should raise an internal error
    assertFailsWith<IllegalStateException> { strategy.onCompleted(1) }
  }

  @Test
  fun `that an invocation can end with error`() {
    var requestId: Long = 0
    val invocations = MockInvocations {
      requestId = it.id
      log.info("invoking", it)
      Future.succeededFuture()
    }
    val strategy = FutureInvocationStrategy(invocations, TestInterface::testFuture.name, TestInterface::testFuture.javaMethod?.genericReturnType!!, arrayOf())
    val future = strategy.getResult() // kicks off the invocation
    assertEquals(1, invocations.activeRequestsCount, "that there is one active request")
    assertEquals(strategy, invocations.getInvocationStrategy(requestId), "that the strategy is our future strategy")
    assertFalse(future.isComplete, "that the future should not have completed")
    // simulate receiving an exception message that the invocation has never been
    invocations.receive(JsonRPCErrorResponse.internalError(id = 1, message = "exception!"))
    assertEquals(0, invocations.activeRequestsCount, "that there is still one active request")
    assertTrue(future.failed(), "that the future should not have completed")
    assertEquals("exception!", future.cause().message)
  }

  @Test
  fun `that we trap exceptions from a future handler that throws exception on handling an invocation result`() {
    var requestId: Long = 0
    val invocations = MockInvocations {
      requestId = it.id
      log.info("invoking", it)
      Future.succeededFuture()
    }
    val strategy = FutureInvocationStrategy(invocations, TestInterface::testFuture.name, TestInterface::testFuture.javaMethod?.genericReturnType!!, arrayOf())
    val future = strategy.getResult() // kicks off the invocation

    // handler is faulty and throws an exception - we are testing that we correctly report this up
    future.setHandler { throw RuntimeException("oh oh!") }

    assertEquals(1, invocations.activeRequestsCount, "that there is one active request")
    assertEquals(strategy, invocations.getInvocationStrategy(requestId), "that the strategy is our future strategy")
    assertFalse(future.isComplete, "that the future should not have completed")

    // simulate receiving the result
    assertFailsWith<RuntimeException> { strategy.onNext(1, "result") }
  }

  @Test
  fun `that we trap exceptions from a future handler that throws exception on handling an invocation exception`() {
    var requestId: Long = 0
    val invocations = MockInvocations {
      requestId = it.id
      log.info("invoking", it)
      Future.succeededFuture()
    }
    val strategy = FutureInvocationStrategy(invocations, TestInterface::testFuture.name, TestInterface::testFuture.javaMethod?.genericReturnType!!, arrayOf())
    val future = strategy.getResult() // kicks off the invocation

    // handler is faulty and throws an exception - we are testing that we correctly report this up
    future.setHandler { throw RuntimeException("oh oh!") }

    assertEquals(1, invocations.activeRequestsCount, "that there is one active request")
    assertEquals(strategy, invocations.getInvocationStrategy(requestId), "that the strategy is our future strategy")
    assertFalse(future.isComplete, "that the future should not have completed")

    // simulate receiving the result
    assertFailsWith<RuntimeException> { strategy.onError(1, RuntimeException("invocation exception")) }
  }
}

