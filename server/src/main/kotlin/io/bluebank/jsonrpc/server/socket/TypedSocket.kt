package io.bluebank.jsonrpc.server.socket

import io.bluebank.jsonrpc.server.socket.impl.TypedSocketImpl
import io.vertx.core.buffer.Buffer

interface TypedSocket<R> : Socket<R, Any>, SocketListener<Buffer, Buffer> {
  companion object {
    fun <R> create(receivedClass: Class<R>) = TypedSocketImpl(receivedClass)
  }
}

