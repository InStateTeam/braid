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
import org.slf4j.LoggerFactory
import rx.Observable
import rx.Single
import java.util.concurrent.CountDownLatch

private val log = LoggerFactory.getLogger("Async.kt")

fun <T: Any> Single<T>.toFuture() : Future<T> {
  val result = future<T>()
  this.subscribe(result::complete, result::fail)
  return result
}

/**
 * because we think there's a race condition in [Observable.toSingle]
 */
fun <T: Any> Observable<T>.toFuture() : Future<T> {
  val result = future<T>()
  var onlyItem : T? = null

  this.subscribe({ item ->
    try {
      when (onlyItem) {
        null -> onlyItem = item
        else -> result.fail("received $onlyItem but also received a second item $item")
      }
    } catch (err: Throwable) {
      log.error("failed during handling of item in toFuture", err)
    }
  }, { err -> // on error
    try {
      when {
        result.isComplete -> log.warn("received error from observable but future has already been completed")
        else -> result.fail(err)
      }
    } catch(err: Throwable) {
      log.error("failed during handling of error in toFuture", err)
    }
  }, { // on completed
    try {
      when {
        result.failed() -> {
          log.trace("received an observable completion after future has already been failed")
        }
        result.succeeded() -> {
          // this is very bad. we've completed successfully but we've received a second completion message
          log.warn("received message for request that has already been completed with {}", onlyItem)
        }
        else -> {
          // we should have a result now
          when (onlyItem) {
            null -> {
              val msg = "received completed message but didn't receive a result"
              log.error(msg)
              result.fail(msg)
            }
            else -> {
              log.trace("received completion message after one and only item in toFuture")
              result.complete(onlyItem)
            }
          }
        }
      }
    } catch(err: Throwable) {
      log.error("failed in handling observable completion in toFuture", err)
    }
  })
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