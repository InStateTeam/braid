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
package io.bluebank.braid

import io.bluebank.braid.client.BraidClientConfig
import io.bluebank.braid.client.BraidCordaProxyClient
import io.bluebank.braid.client.BraidProxyClient
import io.bluebank.braid.server.ComplexObject
import io.bluebank.braid.server.JsonRPCServer
import io.bluebank.braid.server.JsonRPCServerBuilder.Companion.createServerBuilder
import io.bluebank.braid.server.MyService
import io.bluebank.braid.server.MyServiceImpl
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
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

  // can i put the test here? Seems to call the params stuff!
  @Test
  fun `should be able to add two numbers together`() {
    val result = myService.add(1.0, 2.0)
    assertEquals(0, braidClient.activeRequestsCount())
    assertEquals(3.0, result, 0.0001)
  }

  @Test
  fun `sending a request to a client that has two functions with the same name and number of parameters finds the correct function`() {
    val functionWithABigDecimalParameterResult = myService.functionWithTheSameNameAndNumberOfParameters(BigDecimal("200.12345"), "My Netflix account")
    assertEquals(1, functionWithABigDecimalParameterResult)
    // have to test a non number string since it will otherwise try and convert it to a big decimal
    val functionWithAStringParameterResult = myService.functionWithTheSameNameAndNumberOfParameters("not a number", "My Netflix account")
    assertEquals(2, functionWithAStringParameterResult)

    val functionWithTwoBigDecimalParametersResult = myService.functionWithTheSameNameAndNumberOfParameters(BigDecimal("200.12345"), BigDecimal("200.12345"))
    assertEquals(3, functionWithTwoBigDecimalParametersResult)

    val functionWithBigDecimalAndStringNumberParametersResult = myService.functionWithTheSameNameAndNumberOfParameters(BigDecimal("200.12345"), "200.12345")
    assertEquals(3, functionWithBigDecimalAndStringNumberParametersResult)

    val functionWithLongParametersResult = myService.functionWithTheSameNameAndNumberOfParameters(200L, "100.123")
    assertEquals(4, functionWithLongParametersResult)

    // long always over takes int
    val functionWithIntParametersResult = myService.functionWithTheSameNameAndNumberOfParameters("200.12345", 200)
    assertEquals(5, functionWithIntParametersResult)

    // the double version is always called since it has higher priority
    val functionWithFloatParametersResult = myService.functionWithTheSameNameAndNumberOfParameters(200.1234F, "100.123")
    assertEquals(7, functionWithFloatParametersResult)

    val functionWithDoubleParametersResult = myService.functionWithTheSameNameAndNumberOfParameters(200.1234, "100.123")
    assertEquals(7, functionWithDoubleParametersResult)

    val functionWithComplexObjectParametersResult = myService.functionWithTheSameNameAndNumberOfParameters(ComplexObject("1", 2, 3.0), "100.123")
    assertEquals(8, functionWithComplexObjectParametersResult)

  }

  @Test
  fun `abc`() {
    val result = myService.echoComplexObject(ComplexObject("hi", 0, 1.0))
    assertEquals(0, braidClient.activeRequestsCount())
//    assertEquals(3.0, result, 0.0001)
  }

  private fun getFreePort(): Int {
    return (ServerSocket(0)).use {
      it.localPort
    }
  }
}