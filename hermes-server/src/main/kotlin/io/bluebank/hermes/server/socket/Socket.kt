package io.bluebank.hermes.server.socket

import io.vertx.ext.auth.User

interface Socket<R, S> {
  fun addListener(listener: SocketListener<R, S>): Socket<R, S>
  fun write(obj: S): Socket<R, S>
  fun user(): User?

  fun onData(fn: Socket<R, S>.(item: R) -> Unit) {
    this.addListener(object : SocketListener<R, S> {
      override fun dataHandler(socket: Socket<R, S>, item: R) {
        socket.fn(item)
      }

      override fun endHandler(socket: Socket<R, S>) {
      }

      override fun onRegister(socket: Socket<R, S>) {
      }
    })
  }

  fun onEnd(fn: Socket<R, S>.() -> Unit) {
    this.addListener(object : SocketListener<R, S> {
      override fun dataHandler(socket: Socket<R, S>, item: R) {
      }

      override fun endHandler(socket: Socket<R, S>) {
        socket.fn()
      }

      override fun onRegister(socket: Socket<R, S>) {
      }
    })
  }
}

