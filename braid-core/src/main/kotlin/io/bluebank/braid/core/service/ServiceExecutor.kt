package io.bluebank.braid.core.service

import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import rx.Observable

class MethodDoesNotExist(val methodName: String) : Exception()

interface ServiceExecutor {
  fun invoke(request: JsonRPCRequest) : Observable<Any>
  fun getStubs() : List<MethodDescriptor>
}

data class MethodDescriptor(val name: String, val description: String, val parameters: Map<String, String>, val returnType: String)
