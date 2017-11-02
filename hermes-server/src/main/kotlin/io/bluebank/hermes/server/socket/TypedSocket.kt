package io.bluebank.hermes.server.socket

import io.bluebank.hermes.server.socket.impl.TypedSocketImpl
import io.vertx.core.buffer.Buffer

interface TypedSocket<R, K : Any> : SocketAndListener<R, K, Buffer, Buffer> {
  companion object {
    inline fun <reified R, K> create() = TypedSocketImpl<R, K>(R::class.java)
  }
}
