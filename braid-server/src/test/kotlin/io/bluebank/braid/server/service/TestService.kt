package io.bluebank.braid.server.service

import io.bluebank.braid.core.annotation.ServiceDescription
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import rx.Observable

interface MyService {
  fun add(lhs: Double, rhs: Double): Double
  fun noResult()
  fun longRunning() : Future<Void>
  fun stream() : Observable<Int>
}

@ServiceDescription("my-service", "a simple service")
class MyServiceImpl(private val vertx: Vertx) : MyService {
  override fun add(lhs: Double, rhs: Double): Double {
    return lhs + rhs
  }

  override fun noResult() {
  }

  override fun longRunning(): Future<Void> {
    val result = future<Void>()
    vertx.setTimer(100) {
      result.complete()
    }
    return result
  }

  override fun stream(): Observable<Int> {
    return Observable.from(0 .. 10)
  }
}
