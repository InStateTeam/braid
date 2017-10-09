package io.bluebank.jsonrpc.server

import io.bluebank.jsonrpc.server.JsonRPCErrorPayload.Companion.throwInvalidRequest
import io.bluebank.jsonrpc.server.JsonRPCErrorPayload.Companion.throwParseError
import io.bluebank.jsonrpc.server.JsonRPCErrorPayload.Companion.throwServerError
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.Json

class JsonRPCMounter(private val executor: ServiceExecutor, private val socket: ServerWebSocket) {
  class FutureHandler(val callback: (AsyncResult<Any?>) -> Unit) : Handler<AsyncResult<Any?>> {
    override fun handle(event: AsyncResult<Any?>) {
      callback(event)
    }
  }

  init {
    socket.handler {
      handleRequest(it)
    }
    .closeHandler {
      // TODO: maybe release the service?
    }
  }

  private fun handleRequest(it: Buffer) {
    try {
      val request = parse(it)
      checkVersion(request)
      executor.invoke(request) {
        handleAsyncResult(it, request)
      }
    } catch (err: JsonRPCException) {
      err.payload.send()
    }
  }

  private fun handleAsyncResult(result2: AsyncResult<*>, request: JsonRPCRequest) {
    when (result2.succeeded()) {
      true -> respond(result2.result(), request)
      else -> throwServerError(request.id, result2.cause().message)
    }
  }

  private fun respond(result: Any?, request: JsonRPCRequest) {
    val payload = JsonRPCResponsePayload(result = result, id = request.id)
    socket.writeFinalTextFrame(Json.encode(payload))
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

  private fun JsonRPCErrorPayload.send() {
    socket.writeFinalTextFrame(Json.encode(this))
  }
}