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

import io.bluebank.braid.corda.BraidCordaJacksonSwaggerInit
import io.bluebank.braid.corda.rest.RestMounter
import io.bluebank.braid.corda.server.rpc.RPCFactory
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import net.corda.core.utilities.contextLogger
import java.util.concurrent.CountDownLatch

class BraidDocsMain() {
  companion object {
    private val log = contextLogger()

    init {
      BraidCordaJacksonSwaggerInit.init()
    }
  }

  /**
   * @param openApiVersion - 2 or 3
   */
  fun swaggerText(openApiVersion: Int): String {
    val vertx = Vertx.vertx()
    return try {
      val restConfig =
        BraidCordaStandaloneServer(vertx = vertx)
          .createRestConfig(RPCFactory.createRpcFactoryStub())
          .withOpenApiVersion(openApiVersion)
      val restMounter = RestMounter(restConfig, Router.router(vertx), vertx)
      restMounter.docsHandler.getSwaggerString()
    } finally {
      log.info("shutting down Vertx")
      val done = CountDownLatch(1)
      vertx.close {
        log.info("vertx shutdown")
        done.countDown()
      }
      done.await()
    }
  }
}
