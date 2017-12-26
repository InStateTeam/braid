package io.bluebank.braid.core.socket

import io.bluebank.braid.core.socket.impl.SockJsSocketImpl
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.handler.sockjs.SockJSSocket

interface SockJSSocketWrapper : Socket<Buffer, Buffer> {
  companion object {
    /**
     * wrap a vertx [SockJSSocket] with a [Socket] wrapper
     */
    fun create(socket: SockJSSocket) : SockJSSocketWrapper {
      return SockJsSocketImpl(socket)
    }
  }
}

