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
import io.vertx.core.Future
import java.lang.reflect.Type
import kotlin.reflect.KFunction

internal class FutureInvocationStrategy(
  parent: Invocations,
  method: String,
  returnType: Type,
  params: Array<out Any?>
) : InvocationStrategy<Future<Any?>>(parent, method, returnType, params) {
  companion object {
    private val log = loggerFor<FutureInvocationStrategy>()
  }

  private val result = Future.future<Any?>()
  private var requestId : Long = -1

  override fun getResult(): Future<Any?> {
    requestId = invoke()
    return result
  }

  override fun onError(requestId: Long, error: Throwable) {
    assertIdMatch(requestId)
    ifNotCompleted(this::onError) {
      try {
        result.fail(error)
      } catch(err: Throwable) {
        asMDC(requestId) { log.error("handler for invocation failed to handle the error response", err)}
      }
    }
  }

  override fun onNext(requestId: Long, item: Any?) {
    assertIdMatch(requestId)
    ifNotCompleted(this::onNext) {
      try {
        result.complete(item)
      } catch (err: Throwable) {
        asMDC(requestId) { log.error("handler for invocation failed to handle the result response", err)}
      }
    }
  }

  override fun onCompleted(requestId: Long) {
    ifCompleted(this::onCompleted) {
      // nothing to do - we should already be completed at this point
    }
  }

  private fun ifCompleted(context: KFunction<*>, fn: () -> Unit) {
    if (!result.isComplete) {
      asMDC(requestId) {
        log.warn("request should already been completed during ${context.name} but it wasn't")
      }
    }
    fn()
  }

  private fun ifNotCompleted(context: KFunction<*>, fn: () -> Unit) {
    if (result.isComplete) {
      asMDC(requestId) {
        log.warn("request has already been completed during ${context.name} but it shouldn't be")
      }
    }
    fn()
  }

  private fun assertIdMatch(requestId: Long) {
    if (this.requestId != requestId) {
      throw RuntimeException("was asked to process an event for request id $requestId but I was expecting ${this.requestId}")
    }
  }
}
