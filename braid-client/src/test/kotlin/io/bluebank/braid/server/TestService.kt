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
  fun largelyNotStream(): Observable<Int>
  fun echoComplexObject(inComplexObject: ComplexObject): ComplexObject
  fun stuffedJsonObject(): JsonStuffedObject
  fun blowUp()
}

data class ComplexObject(val a: String, val b: Int, val c: Double)

data class JsonStuffedObject(val a: String) {
  val b: String
    get() = a
}

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

  override fun largelyNotStream(): Observable<Int> {
    return Observable.error(RuntimeException("stream error"))
  }

  override fun echoComplexObject(inComplexObject: ComplexObject): ComplexObject {
    return inComplexObject
  }

  override fun blowUp() {
    throw RuntimeException("expected exception")
  }

  override fun stuffedJsonObject(): JsonStuffedObject {
    return JsonStuffedObject("this is hosed")
  }
}
