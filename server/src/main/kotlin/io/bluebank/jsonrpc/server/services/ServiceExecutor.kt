package io.bluebank.jsonrpc.server.services

import io.bluebank.jsonrpc.server.JsonRPCRequest
import rx.Observable

class MethodDoesNotExist : Exception()

interface ServiceExecutor {
  fun invoke(request: JsonRPCRequest) : Observable<Any>
}


