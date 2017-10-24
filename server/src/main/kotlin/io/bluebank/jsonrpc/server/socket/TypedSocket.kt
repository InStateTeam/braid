package io.bluebank.jsonrpc.server.socket

import io.bluebank.jsonrpc.server.socket.impl.TypedSocketImpl
import io.vertx.core.buffer.Buffer

interface TypedSocket<R, K : Any> : Socket<R, K>, SocketListener<Buffer, Buffer> {
  companion object {
    inline fun <reified R, K> create() = TypedSocketImpl<R, K>(R::class.java)
  }
}

