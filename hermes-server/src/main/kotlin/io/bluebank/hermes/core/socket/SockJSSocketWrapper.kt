package io.bluebank.hermes.core.socket

import io.bluebank.hermes.core.socket.impl.SockJsSocketImpl
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.handler.sockjs.SockJSSocket

interface SockJSSocketWrapper : Socket<Buffer, Buffer> {
  companion object {
    fun create(socket: SockJSSocket) : SockJSSocketWrapper {
      return SockJsSocketImpl(socket)
    }
  }
}

