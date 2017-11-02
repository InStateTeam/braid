package io.bluebank.hermes.core.services

import io.bluebank.hermes.core.jsonrpc.JsonRPCRequest
import rx.Observable

class MethodDoesNotExist : Exception()

interface ServiceExecutor {
  fun invoke(request: JsonRPCRequest) : Observable<Any>
}


