package io.bluebank.hermes.server.services

import io.bluebank.hermes.server.JsonRPCRequest
import rx.Observable

class MethodDoesNotExist : Exception()

interface ServiceExecutor {
  fun invoke(request: JsonRPCRequest) : Observable<Any>
}


