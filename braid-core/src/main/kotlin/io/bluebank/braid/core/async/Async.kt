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