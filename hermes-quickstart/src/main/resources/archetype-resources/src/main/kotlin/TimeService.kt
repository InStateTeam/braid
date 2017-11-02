package ${package}

import io.bluebank.hermes.server.MethodDescription
import io.bluebank.hermes.server.ServiceDescription
import io.vertx.core.Vertx
import rx.Observable
import rx.Subscriber
import java.text.SimpleDateFormat
import java.util.*

/**
 * a simple time service
 * This uses publishes "time" events to the event bus
 * Each client of the [time] function listens to the bus
 */
@ServiceDescription("time", "a simple time service")
class TimeService(private val vertx: Vertx) {
  companion object {
    private val timeFormat = SimpleDateFormat("HH:mm:ss")
  }

  init {
    // create a timer publisher to the eventbus
    vertx.setPeriodic(1000) {
      vertx.eventBus().publish("time", timeFormat.format(Date()))
    }
  }

  // N.B. how we can document the return type of a stream
  @MethodDescription(returnType = String::class, description = "return a stream of time updates")
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