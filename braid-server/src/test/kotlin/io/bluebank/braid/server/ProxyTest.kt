package io.bluebank.braid.server

import io.bluebank.braid.client.BraidClientConfig
import io.bluebank.braid.client.BraidProxy
import io.bluebank.braid.server.JsonRPCServerBuilder.Companion.createServerBuilder
import io.bluebank.braid.server.service.ComplexObject
import io.bluebank.braid.server.service.MyService
import io.bluebank.braid.server.service.MyServiceImpl
import io.bluebank.braid.server.util.getFreePort
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.*
import org.junit.runner.RunWith
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(VertxUnitRunner::class)

class ProxyTest {
  private val vertx = Vertx.vertx()
  private val port = getFreePort()
  private lateinit var rpcServer : JsonRPCServer
  private lateinit var braidClient : BraidProxy<MyService>

  private val testLatch = CountDownLatch(1)

  lateinit var myService: MyService

  companion object {
    private val setupLatch = CountDownLatch(1)
  }

  @Before
  fun beforeClass(context: TestContext) {
    rpcServer = createServerBuilder()
        .withVertx(vertx)
        .withService(MyServiceImpl(vertx))
        .withPort(port)
        .build()

    rpcServer.start {
      val serviceURI = URI("https://localhost:$port${rpcServer.rootPath}jsonrpc/my-service")
      braidClient = BraidProxy(MyService::class.java, BraidClientConfig(serviceURI = serviceURI, trustAll = true, verifyHost = false))

      braidClient.bind().map {
        myService = it
        setupLatch.countDown()
      }.setHandler {
        if (it.failed()) {
          println(it.cause().message)
          throw RuntimeException(it.cause())
        }
      }

    }
  }

  @Before
  fun checkSetupLatch() {
    Assert.assertTrue("Woah, setup didn't complete....",setupLatch.await(15, TimeUnit.SECONDS))
  }

  @After
  fun afterClass() {
    rpcServer.stop()
    vertx.close()
  }

  @Test
  fun `should be able to add two numbers together`() {
    val result = myService.add(1.0, 2.0)
    Assert.assertEquals(3.0, result, 0.0001)
  }

//  @Test
//  fun `should be able to get a complex object back from the proxy`() {
//    braidClient.bind().map {
//      val complexObject = ComplexObject("1", 2, 3.0)
//      println("making call")
//      val result = it.echoComplexObject(complexObject)
//      Assert.assertEquals(complexObject, result)
//      latch.countDown()
//    }
//    Assert.assertTrue(latch.await(15, TimeUnit.SECONDS))
//  }
//
//  @Test
//  fun `should be able to get a future back from the proxy`() {
//    braidClient.bind().map {
//      it.longRunning()
//    }.map{
//      it.setHandler { Assert.fail("it")}
//    }
//    latch.await(5, TimeUnit.SECONDS)
//  }

}