package io.bluebank.braid.core.security.impl

import io.bluebank.braid.core.jsonrpc.JsonRPCErrorResponse
import io.bluebank.braid.core.jsonrpc.JsonRPCErrorResponse.Companion.invalidParams
import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.jsonrpc.JsonRPCResultResponse
import io.bluebank.braid.core.security.AuthenticatedSocket
import io.bluebank.braid.core.socket.AbstractSocket
import io.bluebank.braid.core.socket.Socket
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User

class AuthenticatedSocketImpl(private val authProvider: AuthProvider) : AbstractSocket<Buffer, Buffer>(), AuthenticatedSocket {

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
    val op = Json.decodeValue(item, JsonRPCRequest::class.java)
    when (op.method) {
      "login" -> {
        handleAuthRequest(op)
      }
      "logout" -> {
        user = null
        sendOk(op)
      }
      else -> {
        // this isn't an auth op, so if we're logged in, then pass it on
        if (user != null) {
          onData(item)
        } else {
          val msg = JsonRPCErrorResponse.serverError(id = op.id, message = "not authenticated")
          write(Json.encodeToBuffer(msg))
        }
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

  @Suppress("UNCHECKED_CAST")
  private fun handleAuthRequest(op: JsonRPCRequest) {
    if (op.params == null || op.params !is Map<*, *>) {
      sendParameterError(op)
    } else {
      val m = op.params as Map<String, Any>
      authProvider.authenticate(JsonObject(m)) {
        if (it.succeeded()) {
          user = it.result()
          sendOk(op)
        } else {
          user = null
          sendFailed(op, "failed to authenticate")
        }
      }
    }
  }

  private fun sendParameterError(op: JsonRPCRequest) {
    val msg = invalidParams(id = op.id, message = "invalid parameter count for login - expected a single object")
    write(Json.encodeToBuffer(msg))
  }

  private fun sendOk(op: JsonRPCRequest) {
    val msg = JsonRPCResultResponse(id = op.id, result = "OK")
    write(Json.encodeToBuffer(msg))
  }

  private fun sendFailed(op: JsonRPCRequest, cause: Throwable) {
    sendFailed(op, cause.message ?: "unspecified error")
  }

  private fun sendFailed(op: JsonRPCRequest, message: String) {
    val msg = JsonRPCErrorResponse.serverError(id = op.id, message = message)
    write(Json.encodeToBuffer(msg))
  }
}