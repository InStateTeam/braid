package io.bluebank.braid.core.async

import io.vertx.core.Future
import io.vertx.core.Future.future
import rx.Single

fun <T: Any> Single<T>.toFuture() : Future<T> {
  val result = future<T>()
  this.subscribe(result::complete, result::fail)
  return result
}