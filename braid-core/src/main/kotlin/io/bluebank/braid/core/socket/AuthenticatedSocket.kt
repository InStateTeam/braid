package io.bluebank.braid.core.socket

import io.bluebank.braid.core.socket.impl.AuthenticatedSocketImpl
import io.vertx.core.buffer.Buffer
import io.vertx.ext.auth.AuthProvider

interface AuthenticatedSocket : SocketProcessor<Buffer, Buffer, Buffer, Buffer> {
  companion object {
    fun create(authProvider: AuthProvider)  = AuthenticatedSocketImpl(authProvider)
  }
}
