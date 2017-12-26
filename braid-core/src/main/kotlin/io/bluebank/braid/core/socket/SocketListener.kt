package io.bluebank.braid.core.socket

interface SocketListener<R, S> {
  fun onRegister(socket: Socket<R, S>)
  fun dataHandler(socket: Socket<R, S>, item: R)
  fun endHandler(socket: Socket<R, S>)
}