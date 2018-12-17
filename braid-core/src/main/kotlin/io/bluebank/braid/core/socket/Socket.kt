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

import io.vertx.ext.auth.User

/**
 * Abstract for a bi-directional network socket
 * A client of this socket will receive objects of type [R] and send objects of type [S]
 */
interface Socket<R, S> {

  /**
   * called to register a listener on a socket
   * @return this socket
   */
  fun addListener(listener: SocketListener<R, S>): Socket<R, S>

  /**
   * Writes an object into the socket
   * @return this socket
   */
  fun write(obj: S): Socket<R, S>

  /**
   * A socket can have a [User] associated with it. This function returns the user
   */
  fun user(): User?

}

