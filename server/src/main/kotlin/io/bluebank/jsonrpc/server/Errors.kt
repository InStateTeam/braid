package io.bluebank.jsonrpc.server

import io.vertx.core.Future


class JsonRPCException(val payload: JsonRPCErrorPayload) : Exception() {
  @Throws(JsonRPCException::class)
  fun raise() : Nothing {
    throw this
  }
}

class JsonRPCError(val code: Int, val message: String) {
  companion object {
    val PARSE_ERROR = -32700
    val INVALID_REQUEST = -32600
    val METHOD_NOT_FOUND = -32601
    val INVALID_PARAMS = -32602
    val INTERNAL_ERROR = -32603
    val BASE_SERVER_ERROR = -32000 // to -32099
  }
}

data class JsonRPCErrorPayload(val error: JsonRPCError, val id: Any? = null, val jsonrpc: String = "2.0") {
  constructor(id: Any?, message: String, code: Int) : this(JsonRPCError(code, message), id = id)

  companion object {
    fun throwInternalError(id: Any?, message: String) {
      internalError(id, message).raise()
    }

    fun internalError(id: Any?, message: String) =
      JsonRPCErrorPayload(id = id, message = message, code = JsonRPCError.INTERNAL_ERROR).asException()

    @Throws(JsonRPCException::class)
    fun throwParseError(message: String) : Nothing {
      parseError(message).raise()
    }

    fun parseError(message: String) =
      JsonRPCErrorPayload(id = null, message = message, code = JsonRPCError.PARSE_ERROR).asException()

    fun throwInvalidRequest(id: Any?, message: String) {
      invalidRequest(id, message).raise()
    }

    fun invalidRequest(id: Any?, message: String) =
      JsonRPCErrorPayload(id = id, message = message, code = JsonRPCError.INVALID_REQUEST).asException()

    fun throwMethodNotFound(id: Any?, message: String) : Nothing {
      methodNotFound(id, message).raise()
    }

    fun methodNotFound(id: Any?, message: String) =
      JsonRPCErrorPayload(id = id, message = message, code = JsonRPCError.METHOD_NOT_FOUND).asException()

    fun throwInvalidParams(message: String, id: Any? = null) {
      invalidParams(id, message).raise()
    }

    fun invalidParams(id: Any?, message: String) =
      JsonRPCErrorPayload(id = id, message = message, code = JsonRPCError.INVALID_PARAMS).asException()

    fun throwServerError(id: Any?, message: String?, offset: Int = 0) {
      serverError(id, message, offset).raise()
    }

    fun serverError(id: Any?, message: String?, offset: Int = 0) =
      JsonRPCErrorPayload(id = id, message = message ?: "unknown error", code = JsonRPCError.BASE_SERVER_ERROR - offset).asException()

  }

  fun asException() = JsonRPCException(this)
}

fun <T: Any> Throwable.toFailedFuture() : Future<T> = Future.failedFuture<T>(this)
