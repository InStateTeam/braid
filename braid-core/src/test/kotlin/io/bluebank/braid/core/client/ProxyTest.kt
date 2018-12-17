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
import org.junit.*
import org.junit.runner.RunWith
import rx.Observable
import rx.Observable.create

interface Greeter {
  @MethodDescription(returnType = String::class)
  fun greet(name: String, delay: Long): Future<String>

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
      val wrapper = SockJSSocketWrapper.create(it, rule.vertx())
      val typedSocket = TypedSocket.create<JsonRPCRequest, JsonRPCResponse>()
      val mounter = JsonRPCMounter(
        ConcreteServiceExecutor(GreeterService(rule.vertx())),
        rule.vertx()
      )

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
  }

  fun foo() {
  }

  @Ignore // TODO: Fix this
  @Test
  fun `that we can create a proxy and invoke a simple RPC request`(context: TestContext) {
    val greeter =
      Greeter::class.braidProxy(ServiceEndpoint(ssl = false, port = port, path = path))
    val name = "fred"
    greeter.greet(name, 500).setHandler(context.asyncAssertSuccess())
  }
}