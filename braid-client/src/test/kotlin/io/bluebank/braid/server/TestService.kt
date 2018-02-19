package io.bluebank.braid.server.service

import io.bluebank.braid.core.annotation.ServiceDescription
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import rx.Observable

interface MyService {
  fun add(lhs: Double, rhs: Double): Double
  fun noArgs(): Int
  fun noResult()
  fun longRunning() : Future<Int>
  fun stream() : Observable<Int>
  fun echoComplexObject(inComplexObject: ComplexObject): ComplexObject
}

data class ComplexObject(val a: String, val b: Int, val c: Double)

@ServiceDescription("my-service", "a simple service")
class MyServiceImpl(private val vertx: Vertx) : MyService {
  override fun add(lhs: Double, rhs: Double): Double {
    return lhs + rhs
  }

  override fun noArgs(): Int {
    return 5
  }

  override fun noResult() {
  }

  override fun longRunning(): Future<Int> {
    val result = future<Int>()
    vertx.setTimer(100) {
      result.complete(5)
    }
    return result
  }

  override fun stream(): Observable<Int> {
    return Observable.from(0 .. 10)
  }

  override fun echoComplexObject(inComplexObject: ComplexObject): ComplexObject {
    return inComplexObject
  }
}
