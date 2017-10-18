package io.bluebank.jsonrpc.server.services.impl

import io.bluebank.jsonrpc.server.JsonRPCRequest
import io.bluebank.jsonrpc.server.services.ServiceExecutor
import io.vertx.core.AsyncResult
import io.vertx.core.Future

class CompositeExecutor(vararg predefinedExecutors: ServiceExecutor) : ServiceExecutor {
  val executors = mutableListOf(*predefinedExecutors)

  fun add(executor: ServiceExecutor) {
    executors += executor
  }

  override fun invoke(request: JsonRPCRequest, callback: (AsyncResult<Any>) -> Unit) {
    if (executors.isEmpty()) {
      callback(Future.failedFuture("no services available to call via executor interface"))
    } else {
      invoke(0, request, callback)
    }
  }

  private fun invoke(executorIndex: Int, rpcRequest: JsonRPCRequest, resultCallback: (AsyncResult<Any>) -> Unit) {
    // assert executorIndex will always be within range
    executors[executorIndex].invoke(rpcRequest) { ar ->
      if (ar.succeeded()) {
        resultCallback(ar)
      } else {
        if (executorIndex == executors.size - 1) {
          resultCallback(ar)
        } else {
          invoke(executorIndex + 1, rpcRequest, resultCallback)
        }
      }
    }
  }
}