package io.bluebank.braid.core.jsonrpc

import io.bluebank.braid.core.socket.AbstractSocket
import io.bluebank.braid.core.socket.Socket
import io.vertx.ext.auth.User
import java.util.concurrent.atomic.AtomicInteger

class MockSocket<In, Out> : AbstractSocket<In, Out>() {
  private val counter = AtomicInteger(0)
  private val responseListeners = mutableListOf<(Out) -> Unit>()

  val count get() = counter.get()

  internal fun process(request: In) {
    onData(request)
  }

  internal fun addResponseListener(fn: (Out) -> Unit) {
    responseListeners.add(fn)
  }

  override fun write(obj: Out): Socket<In, Out> {
    counter.incrementAndGet()
    responseListeners.forEach { it(obj) }
    return this
  }

  override fun user(): User? {
    return null
  }

  fun end() {
    super.onEnd()
  }
}