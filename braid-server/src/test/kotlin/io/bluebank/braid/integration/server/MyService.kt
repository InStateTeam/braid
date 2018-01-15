package io.bluebank.braid.integration.server

import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import rx.Observable
import rx.Subscriber

class MyService(private val vertx: Vertx) {
  fun add (lhs: Int, rhs: Int)  = lhs + rhs

  fun badjuju() : Int {
    throw RuntimeException("I threw an exception")
  }

  fun asyncResult() : Future<String> {
    val result = future<String>()
    vertx.setTimer(1) {
      result.complete("result as promised")
    }
    return result
  }

  fun streamedResult() : Observable<Int> {
    return Observable.create {
      countdown(10, it)
    }
  }

  private fun countdown(value: Int, subscriber: Subscriber<in Int>) {
    if (value > 0) {
      vertx.setTimer(1) {
        subscriber.onNext(value)
        countdown(value - 1, subscriber)
      }
    } else {
      subscriber.onCompleted()
    }
  }
}