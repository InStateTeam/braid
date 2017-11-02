package io.bluebank.hermes.server

import java.lang.reflect.Method
import java.lang.reflect.Modifier

data class JsonRPCRequest(val jsonrpc: String = "2.0", val id: Long, val method: String, val params: Any?, val streamed: Boolean = false) {
  private val parameters = Params.build(params)

  fun matchesMethod(method: Method): Boolean {
    return Modifier.isPublic(method.modifiers) && (method.name == this.method) && parameters.match(method)
  }

  fun mapParams(method: Method): Array<Any?> {
    return parameters.mapParams(method).toTypedArray()
  }
}