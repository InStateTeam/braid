/**
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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