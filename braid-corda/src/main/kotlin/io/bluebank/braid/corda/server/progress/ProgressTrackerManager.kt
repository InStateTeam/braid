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
package io.bluebank.braid.corda.server.progress

import net.corda.core.messaging.FlowProgressHandle
import rx.Observer
import rx.Subscription

class ProgressTrackerManager {
  private val trackers: MutableMap<String, FlowProgressHandle<*>> = HashMap()
  private val subscribers: MutableList<(Progress) -> Unit> = ArrayList()


  fun put(invocationId: String, tracker: FlowProgressHandle<*>) {
    trackers.put(invocationId, tracker)
    tracker.progress.subscribe(MyObserver(subscribers, invocationId))
  }

  fun subscribe(onNext: (Progress) -> Unit): Subscription {
    subscribers.add(onNext);
    return MySubscription(subscribers,onNext)
  }

  private class MyObserver(val subscribers: List<(Progress) -> Unit>, val invocationId: String) : Observer<String>{
    override fun onError(error: Throwable?) {
      subscribers.forEach{ it.invoke(Progress().withInvocationId(invocationId).withError(error))}
    }

    override fun onNext(step: String?) {
      subscribers.forEach{ it.invoke(Progress().withInvocationId(invocationId).withStep(step)) }
    }

    override fun onCompleted() {
      subscribers.forEach{ it.invoke(Progress().withInvocationId(invocationId).withComplete(true)) }
    }
  }

  private class MySubscription(val subscribers: MutableList<(Progress) -> Unit>, val onNext: (Progress) -> Unit) : Subscription{
    override fun isUnsubscribed(): Boolean {
      return !subscribers.contains { onNext }
    }

    override fun unsubscribe() {
      subscribers.remove(onNext)
    }

  }

}