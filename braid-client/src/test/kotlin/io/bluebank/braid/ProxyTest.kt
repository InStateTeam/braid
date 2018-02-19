package io.bluebank.braid.server

import io.bluebank.braid.client.BraidClientConfig
import io.bluebank.braid.client.BraidProxy
import io.bluebank.braid.server.JsonRPCServerBuilder.Companion.createServerBuilder
import io.bluebank.braid.server.service.ComplexObject
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
import java.util.concurrent.atomic.AtomicInteger

// TODO - why is this blowing up while shutting down?

@RunWith(VertxUnitRunner::class)
class ProxyTest {
  private val vertx = Vertx.vertx()
  private val port = getFreePort()
  private lateinit var rpcServer : JsonRPCServer
  private lateinit var braidClient : BraidProxy<MyService>

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
      braidClient = BraidProxy(MyService::class.java, BraidClientConfig(serviceURI = serviceURI, trustAll = true, verifyHost = false))

      braidClient.bind().map {
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
    rpcServer.stop()
    vertx.close(context.asyncAssertSuccess())
  }

  @Test
  fun `should be able to add two numbers together`() {
    val result = myService.add(1.0, 2.0)
    Assert.assertEquals(3.0, result, 0.0001)
  }

  @Test
  fun `should be able to get a complex object back from the proxy`() {
    val complexObject = ComplexObject("1", 2, 3.0)
    val result = myService.echoComplexObject(complexObject)
    Assert.assertEquals(complexObject, result)
  }

  @Test
  fun `should be able to call method with no arguments`() {
    val result = myService.noArgs()
    Assert.assertEquals(5, result)
  }

  @Test
  fun `should be able to get a future back from the proxy`(context: TestContext) {
    myService.longRunning().map{
      context.assertEquals(5, it)
    }.setHandler(context.asyncAssertSuccess())
  }

  @Test
  fun `should be able to get a stream of events back from the proxy`(context: TestContext) {
    val sequence = AtomicInteger(0)
    val async = context.async()

    myService.stream().subscribe({
      context.assertEquals(sequence.getAndIncrement(), it, "incorrect message received")
    }, {
      context.fail(it.message)
    }, {
      context.assertEquals(11, sequence.get())
      async.complete()
    })
  }

  private fun getFreePort(): Int {
    return (ServerSocket(0)).use {
      it.localPort
    }
  }
}