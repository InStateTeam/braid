package io.bluebank.hermes.server.socket

import io.bluebank.hermes.server.socket.impl.AuthenticatedSocketImpl
import io.vertx.core.buffer.Buffer
import io.vertx.ext.auth.AuthProvider

interface AuthenticatedSocket : SocketAndListener<Buffer, Buffer, Buffer, Buffer> {
  companion object {
    fun create(authProvider: AuthProvider)  = AuthenticatedSocketImpl(authProvider)
  }
}
