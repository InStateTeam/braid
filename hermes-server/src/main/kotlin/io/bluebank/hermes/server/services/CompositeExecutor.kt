package io.bluebank.hermes.server.services

import io.bluebank.hermes.core.jsonrpc.JsonRPCRequest
import io.bluebank.hermes.core.service.MethodDescriptor
import io.bluebank.hermes.core.service.ServiceExecutor
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
      invoke (0, request)
    }
  }

  override fun getStubs(): List<MethodDescriptor> = executors.flatMap { it.getStubs() }

  private fun invoke(executorIndex: Int, rpcRequest: JsonRPCRequest) : Observable<Any> {
    return executors[executorIndex].invoke(rpcRequest)
        .onErrorResumeNext({ err ->
          if (executorIndex == executors.size - 1) {
            Observable.error(err)
          } else {
            invoke(executorIndex + 1, rpcRequest)
          }
        })
  }
}