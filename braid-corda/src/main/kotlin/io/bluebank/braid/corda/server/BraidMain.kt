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
package io.bluebank.braid.corda.server

import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.utils.toJarsClassLoader
import io.bluebank.braid.core.utils.tryWithClassLoader
import io.vertx.core.Future
import net.corda.core.utilities.NetworkHostAndPort

private val log = loggerFor<BraidMain>()

class BraidMain {

  fun start(
    networkAndPort: String,
    userName: String,
    password: String,
    port: Int,
    additionalPaths: List<String>
  ): Future<String> {
    val classLoader = additionalPaths.toJarsClassLoader()
    return tryWithClassLoader(classLoader) {
      Braid(
        port = port,
        userName = userName,
        password = password,
        nodeAddress = NetworkHostAndPort.parse(networkAndPort)
      )
        .startServer()
        .recover {
          log.error("Server failed to start:", it)
          Future.succeededFuture("-1")
        }
    }
  }
}