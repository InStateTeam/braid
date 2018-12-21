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
package io.bluebank.braid.core.jsonrpc

import io.bluebank.braid.core.socket.AbstractSocket
import io.bluebank.braid.core.socket.Socket
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AbstractUser
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import java.util.concurrent.atomic.AtomicInteger

class MockSocket<In, Out>(private val user: User? = null) : AbstractSocket<In, Out>() {
  private val writeCounter = AtomicInteger(0)
  private val responseListeners = mutableListOf<(Out) -> Unit>()

  val writeCount get() = writeCounter.get()

  internal fun process(request: In) {
    onData(request)
  }

  internal fun addResponseListener(fn: (Out) -> Unit) {
    responseListeners.add(fn)
  }

  override fun write(obj: Out): Socket<In, Out> {
    writeCounter.incrementAndGet()
    responseListeners.forEach { it(obj) }
    return this
  }

  override fun user(): User? {
    return user
  }

  fun end() {
    super.onEnd()
  }
}

class MockUser(private val userId : String = "fred") : AbstractUser() {
  override fun doIsPermitted(
    permission: String?,
    resultHandler: Handler<AsyncResult<Boolean>>?
  ) {
    resultHandler?.handle(Future.succeededFuture(true))
  }

  override fun setAuthProvider(authProvider: AuthProvider?) {
  }

  override fun principal(): JsonObject {
    return JsonObject().put("id", userId)
  }
}
