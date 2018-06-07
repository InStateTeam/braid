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
package io.bluebank.braid.core.socket

import io.vertx.core.Vertx
import io.vertx.ext.auth.User
import java.util.concurrent.atomic.AtomicInteger

/**
 * Ensures that all callbacks are not on the main event loop thread
 * Requests are queued in sequence on a separate thread.
 */
class NonBlockingSocket<R, S>(
  vertx: Vertx,
  threads : Int = Math.max(1, Runtime.getRuntime().availableProcessors() - 1), // consume all but one processors
  val ordered: Boolean = false,
  maxExecutionTime : Long = DEFAULT_MAX_EXECUTION_TIME
) : AbstractSocket<R, S>(), SocketProcessor<R, S, R, S> {
  companion object {
    const val DEFAULT_MAX_EXECUTION_TIME = 60L * 60 * 1000 * 1000000 // 1 hour max execution time. Too long?
    private val fountain by lazy {
      val atomic = AtomicInteger(0)
      fun() = "nonblocking-socket-${atomic.getAndIncrement()}"
    }
    /**
     * Default for the maximum number of threads for processing a socket
     * This is set to the max(1, core_count - 1)
     */
    val DEFAULT_MAX_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() - 1)
  }
  private lateinit var socket: Socket<R, S>
  private val pool = vertx.createSharedWorkerExecutor(fountain(), threads, maxExecutionTime)

  override fun onRegister(socket: Socket<R, S>) {
    this.socket = socket
  }

  override fun user(): User? = socket.user()


  override fun dataHandler(socket: Socket<R, S>, item: R) {
    pool.executeBlocking<R>({ onData(item) }, ordered, { })
  }

  override fun endHandler(socket: Socket<R, S>) {
    pool.executeBlocking<Unit>({ onEnd() }, ordered, { })
  }

  override fun write(obj: S): Socket<R, S> {
    socket.write(obj)
    return this
  }

  override fun onEnd(fn: Socket<R, S>.() -> Unit) {
    pool.close()
    super<AbstractSocket>.onEnd(fn)
  }
}