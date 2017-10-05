package io.bluebank.jsonrpc

import io.bluebank.jsonrpc.JsonRPCError.Companion.INTERNAL_ERROR
import io.bluebank.jsonrpc.JsonRPCError.Companion.BASE_SERVER_ERROR
import io.bluebank.jsonrpc.JsonRPCError.Companion.INVALID_PARAMS
import io.bluebank.jsonrpc.JsonRPCError.Companion.INVALID_REQUEST
import io.bluebank.jsonrpc.JsonRPCError.Companion.METHOD_NOT_FOUND
import io.bluebank.jsonrpc.JsonRPCError.Companion.PARSE_ERROR

class JsonRPCException(val payload: JsonRPCErrorPayload) : Exception()

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
    fun internalError(message: String, id: Any? = null): Nothing {
      JsonRPCErrorPayload(id = id, message = message, code = INTERNAL_ERROR).raise()
    }

    fun parseError(message: String, id: Any? = null): Nothing {
      JsonRPCErrorPayload(id = id, message = message, code = PARSE_ERROR).raise()
    }

    fun invalidRequest(message: String, id: Any? = null): Nothing {
      JsonRPCErrorPayload(id = id, message = message, code = INVALID_REQUEST).raise()
    }

    fun methodNotFound(message: String, id: Any? = null): Nothing {
      JsonRPCErrorPayload(id = id, message = message, code = METHOD_NOT_FOUND).raise()
    }

    fun invalidParams(message: String, id: Any? = null): Nothing {
      JsonRPCErrorPayload(id = id, message = message, code = INVALID_PARAMS).raise()
    }

    fun serverError(message: String?, offset: Int = 0, id: Any? = null) : Nothing {
      JsonRPCErrorPayload(id = id, message = message ?: "unknown error", code = BASE_SERVER_ERROR - offset).raise()
    }
  }

  fun raise(): Nothing {
    throw JsonRPCException(this)
  }
}
