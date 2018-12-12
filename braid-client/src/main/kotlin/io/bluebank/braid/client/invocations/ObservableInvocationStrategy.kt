package io.bluebank.braid.client.invocations

import io.bluebank.braid.core.jsonrpc.asMDC
import io.bluebank.braid.core.logging.loggerFor
import rx.Observable
import rx.Subscriber
import java.lang.reflect.Type
import java.util.concurrent.ConcurrentHashMap

internal class ObservableInvocationStrategy(
  parent: Invocations,
  method: String,
  returnType: Type,
  params: Array<out Any?>
) : InvocationStrategy<Observable<Any>>(parent, method, returnType, params) {
  companion object {
    private val log = loggerFor<ObservableInvocationStrategy>()
  }

  private val result : Observable<Any> = Observable.create<Any>(this::onSubscribe)
  private val subscribers = ConcurrentHashMap<Long, Subscriber<Any>>()

  override fun getResult() = result

  private fun onSubscribe(subscriber: Subscriber<Any>) {
    val requestId = nextRequestId()
    subscribers[requestId] = subscriber
    invoke(requestId)
  }

  override fun onNext(requestId: Long, item: Any?) {
    if (item == null) {
      throw RuntimeException("received null item for an Observable")
    }
    getSubscriber(requestId).apply {
      try {
        onNext(item)
      } catch(err: Throwable) {
        asMDC(requestId) {
          log.error("client failed to handle a new item", item)
        }
      }
    }
  }

  override fun onError(requestId: Long, error: Throwable) {
    getSubscriber(requestId).apply {
      try {
        onError(error)
      } catch(err: Throwable) {
        asMDC(requestId) {
          log.error("client failed to handle error response", err)
        }
      }
    }
  }

  override fun onCompleted(requestId: Long) {
    getSubscriber(requestId).apply {
      try {
        onCompleted()
      } catch(err: Throwable) {
        asMDC(requestId) {
          log.error("client failed to handle stream completion", err)
        }
      }
    }
  }

  private fun getSubscriber(requestId: Long) : Subscriber<Any> {
    return subscribers[requestId] ?: throw RuntimeException("failed to locate subscriber for request id $requestId")
  }
}