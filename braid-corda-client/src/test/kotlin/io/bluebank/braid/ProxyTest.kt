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

import io.bluebank.braid.client.BraidClientConfig
import io.bluebank.braid.client.BraidCordaProxyClient
import io.bluebank.braid.client.BraidProxyClient
import io.bluebank.braid.server.JsonRPCServerBuilder.Companion.createServerBuilder
import io.bluebank.braid.server.service.MyService
import io.bluebank.braid.server.service.MyServiceImpl
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.ServerSocket
import java.net.URI

@RunWith(VertxUnitRunner::class)
class ProxyTest {
  private val vertx = Vertx.vertx()
  private val clientVertx = Vertx.vertx()
  private val port = getFreePort()
  private lateinit var rpcServer : JsonRPCServer
  private lateinit var braidClient : BraidProxyClient

  lateinit var myService: MyService

  @Before
  fun before(context: TestContext) {
    val async = context.async()
    rpcServer = createServerBuilder()
        .withVertx(vertx)
        .withService(MyServiceImpl(vertx))
        .withPort(port)
        .build()

    rpcServer.start {
      val serviceURI = URI("https://localhost:$port${rpcServer.rootPath}my-service/braid")
      braidClient = BraidCordaProxyClient(BraidClientConfig(serviceURI = serviceURI, trustAll = true, verifyHost = false), clientVertx)

      braidClient.bindAsync(MyService::class.java).map {
        myService = it
        async.complete()
      }.setHandler {
        if (it.failed()) {
          println(it.cause().message)
          throw RuntimeException(it.cause())
        }
      }
    }

    async.awaitSuccess()
  }

  @After
  fun after(context: TestContext) {
    vertx.close(context.asyncAssertSuccess())
    braidClient.close()
  }

  @Test
  fun `should be able to add two numbers together`() {
    val result = myService.add(1.0, 2.0)
    Assert.assertEquals(0, braidClient.activeRequestsCount())
    Assert.assertEquals(3.0, result, 0.0001)
  }

  private fun getFreePort(): Int {
    return (ServerSocket(0)).use {
      it.localPort
    }
  }
}