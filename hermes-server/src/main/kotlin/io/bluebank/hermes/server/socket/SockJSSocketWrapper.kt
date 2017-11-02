package io.bluebank.hermes.server.socket

import io.bluebank.hermes.server.socket.impl.SockJsSocketImpl
import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.handler.sockjs.SockJSSocket

interface SockJSSocketWrapper : Socket<Buffer, Buffer> {
  companion object {
    fun create(socket: SockJSSocket) : SockJSSocketWrapper {
      return SockJsSocketImpl(socket)
    }
  }
}

