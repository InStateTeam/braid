package io.bluebank.jsonrpc.server.socket

import io.bluebank.jsonrpc.server.AbstractSocket
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User

class AuthenticatedSocket(private val authProvider: AuthProvider) : AbstractSocket<Buffer, Buffer>(), SocketListener<Buffer, Buffer> {

  private var user: User? = null
  private lateinit var socket: Socket<Buffer, Buffer>


  override fun user(): User? {
    return user
  }

  override fun onRegister(socket: Socket<Buffer, Buffer>) {
    this.socket = socket
    this.user = null
  }

  override fun dataHandler(socket: Socket<Buffer, Buffer>, item: Buffer) {
    try {
      val op = Json.decodeValue(item, AuthOp::class.java)
      when (op.operation) {
        Operation.LOGIN -> {
          handleAuthRequest(op)
        }
        Operation.LOGOUT -> {
          user = null
        }
      }
    } catch (err: Throwable) {
      // this isn't an auth op, so if we're logged in, then pass it on
      if (user != null) {
        onData(item)
      } else {
        socket.write(Json.encodeToBuffer("failed because not logged in"))
      }
    }
  }

  override fun endHandler(socket: Socket<Buffer, Buffer>) {
    onEnd()
  }

  override fun write(obj: Buffer): Socket<Buffer, Buffer> {
    socket.write(obj)
    return this
  }

  private fun handleAuthRequest(op: AuthOp) {
    authProvider.authenticate(JsonObject(op.credentials)) {
      if (it.succeeded()) {
        user = it.result()
        socket.write(Json.encodeToBuffer("OK"))
      } else {
        user = null
        socket.write(Json.encodeToBuffer("ERROR - ${it.cause().message ?: "unspecified error"}"))
      }
    }
  }

  private enum class Operation {
    LOGIN,
    LOGOUT
  }

  private data class AuthOp(val operation: Operation, val credentials: Map<String, Any>)
}