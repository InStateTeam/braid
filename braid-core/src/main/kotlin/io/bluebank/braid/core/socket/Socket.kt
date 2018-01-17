/*
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

import io.vertx.ext.auth.User

interface Socket<R, S> {
  fun addListener(listener: SocketListener<R, S>): Socket<R, S>
  fun write(obj: S): Socket<R, S>
  fun user(): User?

  fun onData(fn: Socket<R, S>.(item: R) -> Unit) {
    this.addListener(object : SocketListener<R, S> {
      override fun dataHandler(socket: Socket<R, S>, item: R) {
        socket.fn(item)
      }

      override fun endHandler(socket: Socket<R, S>) {
      }

      override fun onRegister(socket: Socket<R, S>) {
      }
    })
  }

  fun onEnd(fn: Socket<R, S>.() -> Unit) {
    this.addListener(object : SocketListener<R, S> {
      override fun dataHandler(socket: Socket<R, S>, item: R) {
      }

      override fun endHandler(socket: Socket<R, S>) {
        socket.fn()
      }

      override fun onRegister(socket: Socket<R, S>) {
      }
    })
  }
}

