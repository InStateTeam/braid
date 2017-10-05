package io.bluebank.jsonrpc

import java.lang.reflect.Method
import java.lang.reflect.Parameter

data class JsonRPCRequest(val jsonrpc: String, val id: Int, val method: String, val params: Any?) {
  private val parameters = Params.build(params)

  fun matchesMethod(method: Method): Boolean {
    return (method.name == this.method) && parameters.match(method)
  }

  fun mapParams(method: Method): Array<Any?> {
    return parameters.mapParams(method).toTypedArray()
  }
}