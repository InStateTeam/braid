package io.bluebank.braid.server.services

import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.service.MethodDescriptor
import io.bluebank.braid.core.service.MethodDoesNotExist
import io.bluebank.braid.core.service.ServiceExecutor
import rx.Observable

class CompositeExecutor(vararg predefinedExecutors: ServiceExecutor) : ServiceExecutor {
  val executors = mutableListOf(*predefinedExecutors)

  fun add(executor: ServiceExecutor) {
    executors += executor
  }

  override fun invoke(request: JsonRPCRequest): Observable<Any> {
    return if (executors.isEmpty()) {
      Observable.error(RuntimeException("no services available to call via executor interface"))
    } else {
      invoke(0, request)
    }
  }

  override fun getStubs(): List<MethodDescriptor> = executors.flatMap { it.getStubs() }

  private fun invoke(executorIndex: Int, rpcRequest: JsonRPCRequest): Observable<Any> {
    return executors[executorIndex].invoke(rpcRequest)
        .onErrorResumeNext({ err ->
          when {
            err is MethodDoesNotExist && executorIndex < executors.size - 1 ->
              invoke(executorIndex + 1, rpcRequest)
            else -> Observable.error(err)
          }
        })
  }
}