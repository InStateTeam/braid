package io.bluebank.jsonrpc.sample

import io.bluebank.jsonrpc.server.JsonRPCService
import io.vertx.core.Vertx
import rx.Observable
import java.text.SimpleDateFormat
import java.util.*

/**
 * a simple time service
 */
@JsonRPCService("time", "a simple time service")
class TimeService(private val vertx: Vertx) {
  companion object {
    private val timeFormat = SimpleDateFormat("HH:mm:ss")
  }

  init {
    vertx.setPeriodic(1000) {
      vertx.eventBus().publish("time", timeFormat.format(Date()))
    }
  }

  fun time(): Observable<String> {
    return Observable.create { subscriber ->
      val consumer = vertx.eventBus().consumer<String>("time")

      consumer.handler {
        if (subscriber.isUnsubscribed) {
          consumer.unregister()
        } else {
          subscriber.onNext(it.body())
        }
      }
    }
  }
}