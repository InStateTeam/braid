package io.bluebank.braid.core.client

import io.bluebank.braid.core.annotation.MethodDescription
import io.bluebank.braid.core.http.setupAllowAnyCORS
import io.bluebank.braid.core.jsonrpc.JsonRPCMounter
import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.jsonrpc.JsonRPCResponse
import io.bluebank.braid.core.service.ConcreteServiceExecutor
import io.bluebank.braid.core.socket.SockJSSocketWrapper
import io.bluebank.braid.core.socket.TypedSocket
import io.bluebank.braid.core.socket.findFreePort
import io.vertx.core.Future
import io.vertx.core.Future.future
import io.vertx.core.Vertx
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.core.http.HttpServerOptions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import rx.Observable
import rx.Observable.create

interface Greeter {
  @MethodDescription(returnType = String::class)
  fun greet(name: String, delay: Long) : Future<String>
  @MethodDescription(returnType = String::class)
  fun greetRepeat(name: String, delay: Long): Observable<String>
}

class GreeterService(private val vertx: Vertx) : Greeter {
  override fun greet(name: String, delay: Long): Future<String> {
    return future { future ->
      vertx.setTimer(delay) { future.complete(greet(name)) }
    }
  }

  override fun greetRepeat(name: String, delay: Long): Observable<String> {
    return create({ subscriber ->
      vertx.setPeriodic(delay) { id ->
        if (subscriber.isUnsubscribed) {
          vertx.cancelTimer(id)
        } else {
          subscriber.onNext(greet(name))
        }
      }
    })
  }

  private fun greet(name: String) = "Hello, $name"
}

//@RunWith(VertxUnitRunner.class)
@RunWith(VertxUnitRunner::class)
class ProxyTest {

  @Rule
  @JvmField
  val rule = RunTestOnContext()
  val port = findFreePort()
  val path = "/api/calculator"

  @Before
  fun before() {
    val router = Router.router(rule.vertx())
    router.route().handler(BodyHandler.create())
    router.setupAllowAnyCORS()

    val sockJSHandler = SockJSHandler.create(rule.vertx())
    sockJSHandler.socketHandler {
      val wrapper = SockJSSocketWrapper.create(it)
      val typedSocket = TypedSocket.create<JsonRPCRequest, JsonRPCResponse>()
      val mounter = JsonRPCMounter(ConcreteServiceExecutor(GreeterService(rule.vertx())))

      wrapper.addListener(typedSocket)
      typedSocket.addListener(mounter)
    }
    router.route(path).handler(sockJSHandler)

    val serverOptions = HttpServerOptions().setWebsocketSubProtocols("undefined")
    rule.vertx()
        .createHttpServer(serverOptions)
        .requestHandler(router::accept)
        .listen(port)
  }

  @After
  fun after() {
    val f = this.javaClass.getMethod("foo")
    val rt = f.returnType
    val same = (rt.simpleName == "void")
  }

  fun foo() {

  }

  @Test fun `that we can create a proxy and invoke a simple RPC request`(context: TestContext) {
//    val greeter = Greeter::class.braidProxy(ServiceEndpoint(ssl = false, port = port, path = path))
//    val name = "fred"
//    greeter.greet(name, 500).setHandler(context.asyncAssertSuccess())
  }
}