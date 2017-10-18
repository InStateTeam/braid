package io.bluebank.jsonrpc.server.services

import io.bluebank.jsonrpc.server.JsonRPCRequest
import io.vertx.core.AsyncResult

class MethodDoesNotExist : Exception()

interface ServiceExecutor {
  @Throws(MethodDoesNotExist::class)
  fun invoke(request: JsonRPCRequest, callback: (AsyncResult<Any>) -> Unit)
}


