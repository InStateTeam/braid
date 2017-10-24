package io.bluebank.jsonrpc.server.socket.impl

import io.bluebank.jsonrpc.server.AbstractSocket
import io.bluebank.jsonrpc.server.socket.Socket
import io.bluebank.jsonrpc.server.socket.SocketListener
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json
import io.vertx.ext.auth.User

class TypedSocketImpl<R, K>(private val receiveClass: Class<R>) : AbstractSocket<R, K>(), SocketListener<Buffer, Buffer> {

  private lateinit var socket: Socket<Buffer, Buffer>

  override fun onRegister(socket: Socket<Buffer, Buffer>) {
    this.socket = socket
  }

  override fun user(): User? = socket.user()

  override fun dataHandler(socket: Socket<Buffer, Buffer>, item: Buffer) {
    val decoded = Json.decodeValue(item, receiveClass)
    onData(decoded)
  }

  override fun endHandler(socket: Socket<Buffer, Buffer>) {
    onEnd()
  }

  override fun write(obj: K): Socket<R, K> {
    val s = Json.encodeToBuffer(obj)
    socket.write(s)
    return this
  }
}