package io.bluebank.braid.core.socket

import io.bluebank.braid.core.socket.impl.TypedSocketImpl
import io.vertx.core.buffer.Buffer

interface TypedSocket<R, K : Any> : SocketProcessor<R, K, Buffer, Buffer> {
  companion object {
    inline fun <reified R, K> create() = TypedSocketImpl<R, K>(R::class.java)
  }
}
