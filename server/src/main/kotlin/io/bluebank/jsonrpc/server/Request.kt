package io.bluebank.jsonrpc.server

import java.lang.reflect.Method
import java.lang.reflect.Modifier

data class JsonRPCRequest(val jsonrpc: String, val id: Long, val method: String, val params: Any?) {
  private val parameters = Params.build(params)

  fun matchesMethod(method: Method): Boolean {
    return Modifier.isPublic(method.modifiers) && (method.name == this.method) && parameters.match(method)
  }

  fun mapParams(method: Method): Array<Any?> {
    return parameters.mapParams(method).toTypedArray()
  }
}