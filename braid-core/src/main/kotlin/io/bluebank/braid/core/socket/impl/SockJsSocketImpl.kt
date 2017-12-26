package io.bluebank.braid.core.socket.impl

import io.bluebank.braid.core.socket.AbstractSocket
import io.bluebank.braid.core.socket.SockJSSocketWrapper
import io.vertx.core.buffer.Buffer
import io.vertx.ext.auth.User
import io.vertx.ext.web.handler.sockjs.SockJSSocket

class SockJsSocketImpl(private val sockJS: SockJSSocket) : AbstractSocket<Buffer, Buffer>(), SockJSSocketWrapper {

  init {
    sockJS.handler { onData(it) }
    sockJS.endHandler {
      onEnd()
      sockJS.handler(null)
      sockJS.endHandler(null)
    }
  }

  override fun user(): User? {
    return null // the socket itself doesn't know the user
  }

  override fun write(obj: Buffer) : SockJSSocketWrapper {
    sockJS.write(obj)
    return this
  }
}