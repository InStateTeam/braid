package io.bluebank.jsonrpc.server

import io.bluebank.jsonrpc.server.JsonRPCErrorResponse.Companion.serverError
import io.bluebank.jsonrpc.server.JsonRPCErrorResponse.Companion.throwInvalidRequest
import io.bluebank.jsonrpc.server.JsonRPCErrorResponse.Companion.throwParseError
import io.bluebank.jsonrpc.server.services.MethodDoesNotExist
import io.bluebank.jsonrpc.server.services.ServiceExecutor
import io.bluebank.jsonrpc.server.socket.Socket
import io.bluebank.jsonrpc.server.socket.SocketListener
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json

class JsonRPCMounter(private val executor: ServiceExecutor) : SocketListener<JsonRPCRequest, JsonRPCResponse> {
  private lateinit var socket: Socket<JsonRPCRequest, JsonRPCResponse>

  override fun onRegister(socket: Socket<JsonRPCRequest, JsonRPCResponse>) {
    this.socket = socket
  }

  override fun dataHandler(socket: Socket<JsonRPCRequest, JsonRPCResponse>, item: JsonRPCRequest) {
    handleRequest(item)
  }

  override fun endHandler(socket: Socket<JsonRPCRequest, JsonRPCResponse>) {
    // TODO: deactivate all long running streams
  }

  class FutureHandler(val callback: (AsyncResult<Any?>) -> Unit) : Handler<AsyncResult<Any?>> {
    override fun handle(event: AsyncResult<Any?>) {
      callback(event)
    }
  }

  private fun handleRequest(request: JsonRPCRequest) {
    try {
      checkVersion(request)
      executor.invoke(request) {
        handleAsyncResult(it, request)
      }
    } catch (err: JsonRPCException) {
      err.response.send()
    }
  }

  private fun handleAsyncResult(result: AsyncResult<*>, request: JsonRPCRequest) {
    when (result.succeeded()) {
      true -> respond(result.result(), request)
      else -> {
        if (result.cause() is MethodDoesNotExist) {
          JsonRPCErrorResponse.methodNotFound(request.id, "method ${request.method} not implemented").response.send()
        } else {
          serverError(request.id, result.cause().message).response.send()
        }
      }
    }
  }

  private fun respond(result: Any?, request: JsonRPCRequest) {
    val payload = JsonRPCResultResponse(result = result, id = request.id)
    socket.write(payload)
  }

  private fun checkVersion(request: JsonRPCRequest) {
    if (request.jsonrpc != "2.0") {
      throwInvalidRequest(request.id, "jsonrpc must 2.0")
    }
  }

  private fun parse(it: Buffer): JsonRPCRequest {
    try {
      return Json.decodeValue(it, JsonRPCRequest::class.java)
    } catch (err: Throwable) {
      throwParseError(err.message ?: "failed to parse")
    }
  }

  private fun JsonRPCErrorResponse.send() {
    socket.write(this)
  }
}