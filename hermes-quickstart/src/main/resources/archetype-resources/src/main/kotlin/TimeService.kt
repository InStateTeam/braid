package ${package}

import io.bluebank.hermes.server.MethodDescription
import io.bluebank.hermes.server.ServiceDescription
import io.vertx.core.Vertx
import rx.Observable
import java.text.SimpleDateFormat
import java.util.*

/**
 * a simple time service
 */
@ServiceDescription("time", "a simple time service")
class TimeService(private val vertx: Vertx) {
  companion object {
    private val timeFormat = SimpleDateFormat("HH:mm:ss")
  }

  init {
    vertx.setPeriodic(1000) {
      vertx.eventBus().publish("time", timeFormat.format(Date()))
    }
  }

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