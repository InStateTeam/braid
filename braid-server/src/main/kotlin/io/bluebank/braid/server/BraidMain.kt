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
package io.bluebank.braid.server

import io.bluebank.braid.corda.server.Braid
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.utils.toJarsClassLoader
import io.bluebank.braid.core.utils.tryWithClassLoader
import io.vertx.core.Future
import net.corda.core.utilities.NetworkHostAndPort

private val log = loggerFor<BraidMain>()

fun main(args: Array<String>) {
  if (args.size < 4) {
    throw IllegalArgumentException("Usage: BraidMainKt <node address> <username> <password> <port> [<cordaAppJar1> <cordAppJar2> ....]")
  }

  val networkAndPort = args[0]
  val userName = args[1]
  val password = args[2]
  val port = Integer.valueOf(args[3])
  val additionalPaths = args.asList().drop(4)
  BraidMain().start(networkAndPort, userName, password, port, additionalPaths)
}

class BraidMain {

  fun start(networkAndPort: String,userName: String, password: String, port: Int, additionalPaths: List<String>): Future<String> {
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