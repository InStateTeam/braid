package io.bluebank.braid.core.async

import io.vertx.core.Future
import io.vertx.core.Future.future
import rx.Single
import java.util.concurrent.CountDownLatch

fun <T: Any> Single<T>.toFuture() : Future<T> {
  val result = future<T>()
  this.subscribe(result::complete, result::fail)
  return result
}


fun <T : Any> Future<T>.getOrThrow(): T {
  val latch = CountDownLatch(1)

  this.setHandler {
    latch.countDown()
  }

  latch.await()
  if (this.failed()) {
    throw RuntimeException(this.cause())
  }
  return this.result()
}