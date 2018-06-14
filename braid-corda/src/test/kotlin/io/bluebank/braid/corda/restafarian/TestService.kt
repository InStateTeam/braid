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
package io.bluebank.braid.corda.restafarian

import io.bluebank.braid.corda.BraidConfig
import io.bluebank.braid.core.http.HttpServerConfig
import io.netty.buffer.ByteBuf
import io.netty.handler.codec.http.HttpHeaderValues
import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.StateRef
import net.corda.core.contracts.TransactionState
import net.corda.core.cordapp.CordappProvider
import net.corda.core.flows.FlowLogic
import net.corda.core.messaging.FlowHandle
import net.corda.core.messaging.FlowProgressHandle
import net.corda.core.node.AppServiceHub
import net.corda.core.node.NetworkParameters
import net.corda.core.node.NodeInfo
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.*
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.testing.core.DUMMY_BANK_A_NAME
import net.corda.testing.core.TestIdentity
import java.nio.ByteBuffer
import java.sql.Connection
import java.time.Clock

class TestService {
  fun sayHello() = "hello"
  fun echo(msg: String) = "echo: $msg"
  fun getBuffer(): Buffer = Buffer.buffer("hello")
  fun getByteArray(): ByteArray = Buffer.buffer("hello").bytes
  fun getByteBuf(): ByteBuf = Buffer.buffer("hello").byteBuf
  fun getByteBuffer(): ByteBuffer = Buffer.buffer("hello").byteBuf.nioBuffer()
  fun doubleBuffer(bytes: Buffer): Buffer =
    Buffer.buffer(bytes.length() * 2)
      .appendBytes(bytes.bytes)
      .appendBytes(bytes.bytes)
  @ApiOperation(value = "do something custom", response = String::class)
  @ApiImplicitParams(ApiImplicitParam(name = "foo", value = "foo parameter", dataTypeClass = String::class))
  fun somethingCustom(rc: RoutingContext) {
    rc.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON).setChunked(true).end(Json.encode("custom operation done!"))
  }
}

class TestServiceApp(port: Int, private val service: TestService) {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      TestServiceApp(8080, TestService())
    }
  }

  val server = BraidConfig()
    .withPort(port)
    .withService(service)
    .withHttpServerOptions(HttpServerConfig.defaultServerOptions().setSsl(false))
    .withRestConfig(RestConfig(serviceName = "my-service") {
      this.group("General Ledger") {
        this.get("/hello", service::sayHello)
        this.post("/echo", service::echo)
        this.get("/buffer", service::getBuffer)
        this.get("/bytearray", service::getByteArray)
        this.get("/bytebuf", service::getByteBuf)
        this.get("/bytebuffer", service::getByteBuffer)
        this.post("/doublebuffer", service::doubleBuffer)
        this.get("/custom", service::somethingCustom)
      }
    })
  .bootstrapBraid(TestAppServiceHub())

  fun whenReady(): Future<String> = server.whenReady()
  fun shutdown() = server.shutdown()
}

class TestAppServiceHub: AppServiceHub {
  override val attachments: AttachmentStorage
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val clock: Clock
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val contractUpgradeService: ContractUpgradeService
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val cordappProvider: CordappProvider
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val identityService: IdentityService
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val keyManagementService: KeyManagementService
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val myInfo: NodeInfo
    get() = NodeInfo(listOf(NetworkHostAndPort("localhost", 10001)), listOf(TestIdentity(DUMMY_BANK_A_NAME, 40).identity), 3, 1)
  override val networkMapCache: NetworkMapCache
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val networkParameters: NetworkParameters
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val transactionVerifierService: TransactionVerifierService
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val validatedTransactions: TransactionStorage
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
  override val vaultService: VaultService
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

  override fun <T : SerializeAsToken> cordaService(type: Class<T>): T {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun jdbcSession(): Connection {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun loadState(stateRef: StateRef): TransactionState<*> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun loadStates(stateRefs: Set<StateRef>): Set<StateAndRef<ContractState>> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun recordTransactions(statesToRecord: StatesToRecord, txs: Iterable<SignedTransaction>) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun registerUnloadHandler(runOnStop: () -> Unit) {
  }

  override fun <T> startFlow(flow: FlowLogic<T>): FlowHandle<T> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun <T> startTrackedFlow(flow: FlowLogic<T>): FlowProgressHandle<T> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
