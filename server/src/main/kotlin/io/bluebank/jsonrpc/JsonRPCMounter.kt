package io.bluebank.jsonrpc

import io.bluebank.jsonrpc.JsonRPCErrorPayload.Companion.methodNotFound
import io.bluebank.jsonrpc.JsonRPCErrorPayload.Companion.serverError
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.Json
import java.lang.reflect.Method

class JsonRPCMounter<in T : Any>(private val service: T, private val socket: ServerWebSocket) {
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
      val method = findMethod(request)
      val castedParameters = request.mapParams(method)
      try {
        val result = method.invoke(service, *castedParameters)
        handleResult(result, request)
      } catch (err: Throwable) {
        serverError(err.message, 0, request.id)
      }
    } catch (err: JsonRPCException) {
      err.payload.send()
    }
  }

  private fun handleResult(result: Any?, request: JsonRPCRequest) {
    when (result) {
      is Future<*> -> handleFuture(result, request)
      else -> respond(result, request)
    }
  }

  private fun handleFuture(future: Future<*>, request: JsonRPCRequest) {
    future.setHandler { result: Any ->
      when (result) {
        is AsyncResult<*> -> {
          handleAsyncResult(result, request)
        }
        else -> {
          respond(result, request)
        }
      }
    }
  }

  private fun handleAsyncResult(result2: AsyncResult<*>, request: JsonRPCRequest) {
    when (result2.succeeded()) {
      true -> respond(result2.result(), request)
      else -> serverError(result2.cause().message, 0, request.id)
    }
  }

  private fun respond(result: Any?, request: JsonRPCRequest) {
    val payload = JsonRPCResponsePayload(result = result, id = request.id)
    socket.writeFinalTextFrame(Json.encode(payload))
  }

  private fun checkVersion(request: JsonRPCRequest) {
    if (request.jsonrpc != "2.0") {
      JsonRPCErrorPayload.invalidRequest("jsonrpc must 2.0")
    }
  }

  private fun parse(it: Buffer): JsonRPCRequest {
    try {
      return Json.decodeValue(it, JsonRPCRequest::class.java)
    } catch (err: Throwable) {
      JsonRPCErrorPayload.parseError(err.message ?: "failed to parse")
    }
  }

  private fun JsonRPCErrorPayload.send() {
    socket.writeFinalTextFrame(Json.encode(this))
  }

  private fun findMethod(request: JsonRPCRequest): Method {
    try {
      return service.javaClass.methods.single { request.matchesMethod(it) }
    } catch (err: IllegalArgumentException) {
      methodNotFound("method ${request.method} has multiple implementations with the same number of parameters")
    } catch (err: NoSuchElementException) {
      methodNotFound("could not find method ${request.method}")
    }
  }
}