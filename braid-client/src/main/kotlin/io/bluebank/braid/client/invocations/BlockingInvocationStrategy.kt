package io.bluebank.braid.client.invocations

import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.Future
import java.lang.reflect.Type
import java.util.concurrent.CountDownLatch

internal class BlockingInvocationStrategy(
  parent: Invocations,
  method: String,
  returnType: Type,
  params: Array<out Any?>
) : InvocationStrategy<Any?>(parent, method, returnType, params) {
  private val computation = Future.future<Any?>()
  private val latch = CountDownLatch(1)
  private var requestId = -1L

  companion object {
    private val log = loggerFor<BlockingInvocationStrategy>()
  }

  override fun getResult(): Any? {
    checkIdIsNotSet()
    requestId = invoke()
    latch.await()
    return when {
      !computation.isComplete -> throw IllegalStateException("I should have a result or error for you but but neither condition was met!")
      computation.failed() -> throw computation.cause()
      else -> computation.result()
    }
  }

  override fun onNext(requestId: Long, item: Any?) {
    try {
      checkIdIsSet(requestId)
      checkComputationIsNotComplete()
      computation.complete(item)
      latch.countDown()
    } catch (err: Throwable) {
      log.error("failed during onNext", err)
      throw err
    }
  }

  override fun onError(requestId: Long, error: Throwable) {
    try {
      checkIdIsSet(requestId)
      checkComputationIsNotComplete()
      computation.fail(error)
      latch.countDown()
    } catch (err: Throwable) {
      log.error("failed during on Error", err)
      throw err
    }
  }

  override fun onCompleted(requestId: Long) {
    checkIdIsSet(requestId)
    checkComputationIsComplete()
    // NO OP
  }

  private fun checkIdIsNotSet() {
    if (requestId >= 0) {
      throw IllegalStateException("computation appears to have been invoked!")
    }
  }

  private fun checkIdIsSet(requestId: Long) {
    if (this.requestId < 0) throw IllegalStateException("computation appears not to have been invoked but I received a message for request $requestId")
    if (requestId != this.requestId) throw IllegalStateException("computation was invoked with request ${this.requestId} but I've received a message for request $requestId")
  }

  private fun checkComputationIsNotComplete() {
    if (computation.isComplete) throw IllegalStateException("I received a message for request $requestId but computation is already complete!")
  }

  private fun checkComputationIsComplete() {
    if (!computation.isComplete) throw IllegalStateException("I received a message for completion for request $requestId but I haven't received a result!")
  }
}
