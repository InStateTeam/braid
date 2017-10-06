package io.bluebank.jsonrpc.server

import io.vertx.core.AsyncResult

interface ServiceExecutor {
  @Throws(MethodDoesNotExist::class)
  fun invoke(rpcRequest: JsonRPCRequest, resultCallback: (AsyncResult<Any>)->Unit)
}

class MethodDoesNotExist : Exception()

class CompositeExecutor(vararg private val executors: ServiceExecutor) : ServiceExecutor {
  override fun invoke(rpcRequest: JsonRPCRequest, resultCallback: (AsyncResult<Any>) -> Unit) {
    executors.asSequence().forEach {

    }
  }
}