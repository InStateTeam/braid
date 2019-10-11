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

import io.bluebank.braid.core.async.catch
import io.bluebank.braid.core.async.onSuccess
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.utils.toJarsClassLoader
import io.bluebank.braid.core.utils.tryWithClassLoader
import io.vertx.core.Future
import io.vertx.core.Vertx
import net.corda.core.utilities.NetworkHostAndPort

private val log = loggerFor<BraidMain>()

/**
 * The top level entry point for running braid as an executable
 */
class BraidMain(
  private val jarsClassLoader: ClassLoader = Thread.currentThread().contextClassLoader,
  private val openApiVersion: Int = 3,
  private val vertx: Vertx = Vertx.vertx()
) {

  constructor(
    cordappPaths: List<String> = emptyList(),
    openApiVersion: Int = 3,
    vertx: Vertx = Vertx.vertx()
  ) : this(cordappPaths.toJarsClassLoader(), openApiVersion, vertx)

  /**
   * start a braid server on [vertx] instance
   */
  fun start(
    networkAndPort: String,
    userName: String,
    password: String,
    port: Int
  ): Future<String> {
    return tryWithClassLoader(jarsClassLoader) {
      BraidCordaStandaloneServer(
        port = port,
        userName = userName,
        password = password,
        nodeAddress = NetworkHostAndPort.parse(networkAndPort),
        openApiVersion = openApiVersion,
        vertx = vertx
      )
        .startServer()
    }
  }

  /**
   * Shutdown this BraidMain together with all the instances started
   */
  fun shutdown(): Future<Void> {
    return Future.future<Void>().let { result ->
      log.info("shutting down all braid servers ...")
      vertx.close(result::handle)
      return result.onSuccess {
        log.info("all braid servers shutdown")
      }.catch {
        log.error("failure in shutting down braid servers", it.cause)
      }
    }
  }
}