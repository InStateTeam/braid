package io.bluebank.jsonrpc.server.socket

import io.bluebank.jsonrpc.server.socket.impl.AuthenticatedSocketImpl
import io.vertx.core.buffer.Buffer
import io.vertx.ext.auth.AuthProvider

interface AuthenticatedSocket : Socket<Buffer, Buffer>, SocketListener<Buffer, Buffer> {
  companion object {
    fun create(authProvider: AuthProvider)  = AuthenticatedSocketImpl(authProvider)
  }
}
