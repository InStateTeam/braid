package io.bluebank.braid.corda.integration.cordapp

import io.bluebank.braid.core.annotation.ServiceDescription
import io.vertx.core.Future
import net.corda.core.node.ServiceHub
import rx.Observable
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit

@ServiceDescription("my-service", "A simple service for testing braid")
class CustomService(private val serviceHub: ServiceHub) {
  fun add (lhs: Int, rhs: Int)  = lhs + rhs

  fun badjuju() : Int {
    throw RuntimeException("I threw an exception")
  }

  fun asyncResult() : Future<String> {
    val result = Future.future<String>()
    streamedResult().first().single().subscribe { result.complete(it.toString()) }
    return result
  }

  fun streamedResult() : Observable<Int> {
    return Observable.range(0, 10, Schedulers.computation()).delay(10, TimeUnit.MILLISECONDS)
  }
}