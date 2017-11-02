package io.bluebank.hermes.core.socket

import io.bluebank.hermes.core.socket.impl.AuthenticatedSocketImpl
import io.vertx.core.buffer.Buffer
import io.vertx.ext.auth.AuthProvider

interface AuthenticatedSocket : SocketAndListener<Buffer, Buffer, Buffer, Buffer> {
  companion object {
    fun create(authProvider: AuthProvider)  = AuthenticatedSocketImpl(authProvider)
  }
}
