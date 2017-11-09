package io.bluebank.hermes.corda.services

import io.bluebank.hermes.corda.HermesConfig
import io.bluebank.hermes.core.jsonrpc.JsonRPCRequest
import io.bluebank.hermes.core.service.MethodDoesNotExist
import io.bluebank.hermes.core.service.ServiceExecutor
import net.corda.core.flows.FlowLogic
import net.corda.core.toObservable
import net.corda.node.services.api.ServiceHubInternal
import rx.Observable
import java.lang.reflect.Constructor

class CordaFlowServiceExecutor(private val services: ServiceHubInternal, val config: HermesConfig) : ServiceExecutor {
  override fun invoke(request: JsonRPCRequest): Observable<Any> {
    val flow = config.registeredFlows[request.method]
    return if (flow != null) {
      invoke(request, flow)
    } else {
      Observable.error(MethodDoesNotExist())
    }
  }

  private fun invoke(request: JsonRPCRequest, clazz: Class<out FlowLogic<*>>) : Observable<Any> {
    val constructor = clazz.constructors.firstOrNull { it.matches(request) }
    return if (constructor == null) {
      Observable.error(MethodDoesNotExist())
    } else {
      return Observable.create { subscriber ->
        try {
          val params = request.mapParams(constructor)
          @Suppress("UNCHECKED_CAST")
          val flow = constructor.newInstance(*params) as FlowLogic<Any>
          val sm = services.startFlow(flow)
          sm.resultFuture.toObservable().subscribe({item ->
            subscriber.onNext(item)
          }, {err ->
            subscriber.onError(err)
          }, {
            subscriber.onCompleted()
          })
        } catch (err: Throwable) {
          subscriber.onError(err)
        }
      }
    }
  }
}

private fun Constructor<*>.matches(request: JsonRPCRequest) : Boolean {
  return this.parameterCount == request.paramCount()
}