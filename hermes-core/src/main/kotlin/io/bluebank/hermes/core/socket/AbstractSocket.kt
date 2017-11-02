package io.bluebank.hermes.core.socket

import io.bluebank.hermes.core.logging.loggerFor

abstract class AbstractSocket<R, S> : Socket<R, S> {
  companion object {
    private val logger = loggerFor<AbstractSocket<*, *>>()
  }

  private val listeners = mutableListOf<SocketListener<R, S>>()

  override fun addListener(listener: SocketListener<R, S>): Socket<R, S> {
    listeners += listener
    listener.onRegister(this)
    return this
  }

  protected fun Socket<R, S>.onData(item: R): Socket<R, S> {
    listeners.forEach {
      try {
        it.dataHandler(this, item)
      } catch (err: Throwable) {
        logger.error("failed to dispatch onData", err)
      }
    }
    return this
  }

  protected fun Socket<R, S>.onEnd() {
    listeners.forEach {
      try {
        it.endHandler(this)
      } catch (err: Throwable) {
        logger.error("failed to dispatch onEnd", err)
      }
    }
  }
}