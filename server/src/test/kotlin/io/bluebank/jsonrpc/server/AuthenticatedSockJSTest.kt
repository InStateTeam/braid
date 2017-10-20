package io.bluebank.jsonrpc.server

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions
import io.vertx.ext.auth.shiro.ShiroAuthRealmType
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.handler.sockjs.SockJSSocket
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.core.streams.end
import java.text.SimpleDateFormat
import java.util.*

/**
 * Not an automated test.
 * Demonstrates the principles of a secure eventbus over sockjs
 */
class AuthenticatedSockJSTest : AbstractVerticle() {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      JacksonKotlinInit.init()
      Vertx.vertx().deployVerticle(AuthenticatedSockJSTest())
    }

    private val logger = loggerFor<AuthenticatedSockJSTest>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss")
  }

  override fun start(startFuture: Future<Void>) {
    val router = Router.router(vertx)

    router.route().handler(CookieHandler.create())
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
//    setupAuth(router)
    setupSockJS(router)
    setupTimeService()
    setupStatic(router)

    val PORT = 8080
    vertx.createHttpServer()
      .requestHandler(router::accept)
      .listen(PORT) {
        if (it.succeeded()) {
          logger.info("started on http://localhost:$PORT")
        } else {
          logger.error("failed to startup", it.cause())
        }
        startFuture.completer().handle(it.mapEmpty<Void>())
      }

  }

  private fun setupTimeService() {
    vertx.setPeriodic(1000) {
      vertx.eventBus().publish("time", timeFormat.format(Date()))
    }
  }

  private fun setupStatic(router: Router) {
    router.get().handler(StaticHandler.create("eventbus-test").setCachingEnabled(false).setCacheEntryTimeout(1).setMaxCacheSize(1))
  }

  private fun setupSockJS(router: Router) {
    val sockJSHandler = SockJSHandler.create(vertx)
    sockJSHandler.socketHandler(this::socketHandler)
    router.route("/api/*").handler(sockJSHandler)
  }

  private fun socketHandler(socket : SockJSSocket) {
    SockJSWrapper(socket)
    socket.endHandler { consumer.unregister() }
    consumer.handler { socket.write(it.body()) }
  }

  private fun setupAuth(router: Router) {
    val config = json {
      obj("properties_path" to "classpath:login/shiro.properties")
    }
    val provider = ShiroAuth.create(vertx, ShiroAuthOptions().setConfig(config).setType(ShiroAuthRealmType.PROPERTIES))
    router.route().handler(UserSessionHandler.create(provider))
    router.route("/eventbus/*").handler(BasicAuthHandler.create(provider))
  }
}

interface SocketListener<T> {
  fun dataHandler(item : T)
  fun endHandler()
}

interface Socket<R, in S> {
  fun addListener(listener: SocketListener<R>)
  fun write(obj: S)
  fun user(): User?
}

class SockJSWrapper(private val sockJS : SockJSSocket) : Socket<Buffer, Buffer> {
  private val listeners = mutableListOf<SocketListener<Buffer>>()

  init {
    sockJS.handler { handler(it) }
    sockJS.endHandler { this.endHandler() }
  }

  override fun addListener(listener: SocketListener<Buffer>) {
    listeners += listener
  }

  override fun user(): User? {
    return null
  }

  override fun write(obj: Buffer) {
    sockJS.write(obj)
  }

  private fun handler(item: Buffer) {
    listeners.forEach {
      try {
        it.dataHandler(item)
      } catch (err: Throwable) {
        // TODO: log
      }
    }
  }

  private fun endHandler() {
    listeners.forEach {
      try {
        it.endHandler()
      } catch (err: Throwable) {
        // TODO: log
      }
    }
  }
}


class TypedSocket<R, in S>(
  private val receiveClazz: Class<R>,
  private val socket: Socket<Buffer, Buffer>) : Socket<R, S>, SocketListener<R> {

  override fun dataHandler(item: R) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun addListener(listener: SocketListener<R>) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun user(): User? {

    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }


  override fun write(obj: S) {
    val s = Json.encodeToBuffer(obj)
    socket.write(s)
  }

  private fun handler(buffer: Buffer) {
    val o = Json.decodeValue(buffer, receiveClazz)
    handler?.invoke(o)
  }

  private fun endHandler() {
    endHandler?.invoke()
  }
}

class AuthenticatedSocket(private val authProvider: AuthProvider, private val socket : Socket<Buffer, Buffer>) : Socket<Buffer, Buffer>() {
  private var _user: User? = null

  init {
    socket.handler = this::onReceive
    socket.endHandler = this::onEnd
  }

  override val user: User?
    get() = _user

  override fun write(obj: Buffer) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  private fun onReceive(buffer: Buffer) {
    try {
      val op = Json.decodeValue(buffer, AuthOp::class.java)
      when(op.operation) {
        Operation.LOGIN -> {
          handleAuthRequest(op)
        }
        Operation.LOGOUT -> {
          _user = null
        }
      }
    } catch(err: Throwable) {
      // this isn't an auth op so pass it on
      handler?.invoke(buffer)
    }
  }

  private fun handleAuthRequest(op: AuthOp) {
    authProvider.authenticate(op.credentials) {
      if (it.succeeded()) {
        _user = it.result()
        socket.write(Buffer.buffer("true"))
      } else {
        _user = null
        socket.write(Buffer.buffer("false"))
      }
    }
  }

  private fun onEnd() {
    endHandler?.invoke()
  }

  private enum class Operation {
    LOGIN,
    LOGOUT
  }

  private data class AuthOp(val operation: Operation, val credentials: JsonObject)
}