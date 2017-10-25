package io.bluebank.jsonrpc.server.socket

interface SocketAndListener<R1, S1, R2, S2> : Socket<R1, S1>, SocketListener<R2, S2>

fun <R1, S1, R2, S2> SocketAndListener<R1, S1, R2, S2>.listenTo(socket: Socket<R2, S2>): SocketAndListener<R1, S1, R2, S2> {
  socket.addListener(this)
  return this
}