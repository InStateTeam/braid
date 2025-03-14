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
package io.bluebank.braid.core.security

import io.bluebank.braid.core.security.impl.AuthenticatedSocketImpl
import io.bluebank.braid.core.socket.SocketProcessor
import io.vertx.core.buffer.Buffer
import io.vertx.ext.auth.AuthProvider

interface AuthenticatedSocket : SocketProcessor<Buffer, Buffer, Buffer, Buffer> {
  companion object {
    const val LOGIN_METHOD = "login"
    const val LOGOUT_METHOD = "logout"
    const val MSG_FAILED = "failed to authenticate"
    const val MSG_PARAMETER_ERROR =
      "invalid parameter for login - expected a single object"

    fun create(authProvider: AuthProvider) = AuthenticatedSocketImpl(authProvider)
  }
}
