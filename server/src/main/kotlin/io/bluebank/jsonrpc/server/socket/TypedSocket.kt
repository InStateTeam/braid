package io.bluebank.jsonrpc.server.socket

import io.bluebank.jsonrpc.server.AbstractSocket
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json
import io.vertx.ext.auth.User

class TypedSocket<R>(private val receiveClazz: Class<R>) : AbstractSocket<R, Any>(), SocketListener<Buffer, Buffer> {

  private lateinit var socket: Socket<Buffer, Buffer>

  override fun onRegister(socket: Socket<Buffer, Buffer>) {
    this.socket = socket
  }

  override fun user(): User? = socket.user()

  override fun dataHandler(socket: Socket<Buffer, Buffer>, item: Buffer) {
    val decoded = Json.decodeValue(item, receiveClazz)
    onData(decoded)
  }

  override fun endHandler(socket: Socket<Buffer, Buffer>) {
    onEnd()
  }

  override fun write(obj: Any): Socket<R, Any> {
    val s = Json.encodeToBuffer(obj)
    socket.write(s)
    return this
  }
}