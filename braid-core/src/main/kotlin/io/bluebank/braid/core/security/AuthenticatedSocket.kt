package io.bluebank.braid.core.security

import io.bluebank.braid.core.security.impl.AuthenticatedSocketImpl
import io.bluebank.braid.core.socket.SocketProcessor
import io.vertx.core.buffer.Buffer
import io.vertx.ext.auth.AuthProvider

interface AuthenticatedSocket : SocketProcessor<Buffer, Buffer, Buffer, Buffer> {
  companion object {
    fun create(authProvider: AuthProvider)  = AuthenticatedSocketImpl(authProvider)
  }
}
