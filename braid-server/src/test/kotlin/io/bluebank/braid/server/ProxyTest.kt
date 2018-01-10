package io.bluebank.braid.server

import io.bluebank.braid.client.BraidClientConfig
import io.bluebank.braid.client.BraidProxy
import io.bluebank.braid.server.JsonRPCServerBuilder.Companion.createServerBuilder
import io.bluebank.braid.server.service.MyService
import io.bluebank.braid.server.service.MyServiceImpl
import io.bluebank.braid.server.util.getFreePort
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.*
import org.junit.runner.RunWith
import java.net.URI


@RunWith(VertxUnitRunner::class)

class ProxyTest {
  private val vertx = Vertx.vertx()
  private val port = getFreePort()
  private lateinit var rpcServer : JsonRPCServer
  private lateinit var braidClient : BraidProxy<MyService>


  @Before
  fun beforeClass(context: TestContext) {
    rpcServer = createServerBuilder()
        .withVertx(vertx)
        .withService(MyServiceImpl(vertx))
        .withPort(port)
        .build()
        .start()

    val serviceURI = URI("https://localhost:$port${rpcServer.rootPath}my-service")
    braidClient = BraidProxy(MyService::class.java, BraidClientConfig(serviceURI = serviceURI, trustAll = true, verifyHost = false))
    braidClient.bind().setHandler {
      val service = it.result();
      service.add(1, 2)
    }
  }

  @After
  fun afterClass() {
    rpcServer.stop()
    vertx.close()
  }

  @Test
  fun `to create a proxy from a class`() {
  }
}