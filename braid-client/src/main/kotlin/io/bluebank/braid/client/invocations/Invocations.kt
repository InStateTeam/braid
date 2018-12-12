package io.bluebank.braid.client.invocations

import io.bluebank.braid.client.BraidClientConfig
import io.bluebank.braid.core.async.getOrThrow
import io.bluebank.braid.core.json.BraidJacksonInit
import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.jsonrpc.asMDC
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocket
import io.vertx.core.http.WebSocketFrame
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import java.io.Closeable
import java.lang.reflect.Type
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong


/**
 * Public entry-point for invocations responsible for:
 * - managing socket connections
 * - tracking invocations
 * - dispatching the meat of the invocation and response handling logic to [InvocationStrategy](InvocationStrategy.kt)
 * - provides capabilities for InvocationStrategy:
 * - a number requestId number fountain
 *
 * To initialise requires a [vertx] instance, the [config], and optional connection [exceptionHandler] and [closeHandler]
 */
class Invocations(
  val vertx: Vertx,
  private val config: BraidClientConfig,
  private val exceptionHandler: (Throwable) -> Unit = defaultSocketExceptionHandler(),
  private val closeHandler: (() -> Unit) = defaultSocketCloseHandler(),
  clientOptions: HttpClientOptions = defaultClientHttpOptions
) : Closeable {

  companion object {
    private val log = loggerFor<Invocations>()
    val defaultClientHttpOptions = HttpClientOptions()

    fun defaultSocketCloseHandler() = {
      log.info("closing...")
    }

    fun defaultSocketExceptionHandler() = { error: Throwable ->
      log.error("exception from socket", error)
      // TODO: handle retries?
      // TODO: handle error!
    }

    init {
      BraidJacksonInit.init()
    }
  }

  /**
   * tracks invocation requestIds to the strategies responsible for them
   */
  private val invocations = ConcurrentHashMap<Long, InvocationStrategy<*>>()
  /**
   * number fountain for requestIds
   */
  private val nextRequestId = AtomicLong(1)
  /**
   * connection to the server
   * */
  private var socket: WebSocket? = null

  private val client = vertx.createHttpClient(clientOptions
    .setDefaultHost(config.serviceURI.host)
    .setDefaultPort(config.serviceURI.port)
    .setSsl(config.tls)
    .setVerifyHost(config.verifyHost)
    .setTrustAll(config.trustAll))

  init {
    // set up the websocket with all the required handlers

    val protocol = if (config.tls) "https" else "http"
    val url = URL(protocol, config.serviceURI.host, config.serviceURI.port, "${config.serviceURI.path}/websocket")
    val result = Future.future<Boolean>()
    client.websocket(url.toString(), { sock ->
      try {
        socket = sock
        sock.handler(this::socketHandler)
        sock.exceptionHandler(exceptionHandler)
        sock.closeHandler(closeHandler)
        result.complete(true)
      } catch (err: Throwable) {
        log.error("failed to connect socket to the client stack", err)
        socket = null
        result.fail(err)
      }
    }, { error ->
      log.error("failed to bind to websocket", error)
      try {
        socket = null
        result.fail(error)
      } catch (err: Throwable) {
        log.error("failed to report error on result future", err)
      }
    })
    result.getOrThrow()
  }

  val activeRequestsCount get() = invocations.size

  /**
   * shutdown everything
   * after calling this all calls to [invoke] will fail with [IllegalStateException]
   */
  override fun close() {
    if (socket != null) {
      socket?.close()
      socket = null
    }
    socket?.close()
    client.close()
  }

  /**
   * public entry point to invoke a method. may block depending if the call has synchronous signature
   * may not invoke anything at all if the call returns an [rx.Observable]
   * @param [method] the name of the method
   * @param [returnType] the expected return type of the function being called
   * @param [params] the parameters for the call
   * @return the result of the invocation
   */
  fun invoke(method: String, returnType: Type, params: Array<out Any?>): Any? {
    return InvocationStrategy.invoke(this, method, returnType, params)
  }

  /**
   * generate the next request id
   * thread safe
   */
  internal fun nextRequestId() = nextRequestId.getAndIncrement()

  /**
   * set the invocation [strategy] for a [requestId]
   */
  internal fun assignInvocationStrategyForRequest(requestId: Long, strategy: InvocationStrategy<*>) {
    if (invocations.contains(requestId)) {
      asMDC(requestId) {
        log.error("tried to add a strategy for request $requestId but one already exists!")
      }
    } else {
      log.trace("adding strategy for request $requestId")
      invocations[requestId] = strategy
    }
  }

  /**
   * unset / remove the invocation strategy assigned to [requestId]
   */
  internal fun removeInvocationStrategyForRequest(requestId: Long) {
    if (invocations.containsKey(requestId)) {
      log.trace("removing strategy for request")
      invocations.remove(requestId)
    } else {
      asMDC(requestId) {
        log.error("could not remove strategy for request because none could be found!")
      }
    }
  }

  /**
   * writes a [JsonRPCRequest] [request] on the socket to the server
   * @returns future to indicate if the write was succesful or not
   */
  internal fun write(request: JsonRPCRequest): Future<Unit> {
    if (log.isTraceEnabled) {
      log.trace("writing request to socket {}", Json.encode(request))
    }

    val result = Future.future<Unit>()
    try {
      vertx.runOnContext {
        try {
          socket
            ?.writeFrame(WebSocketFrame.textFrame(Json.encode(request), true))
            ?: throw java.lang.IllegalStateException("socket was not created or was closed")
        } catch (err: Throwable) {
          asMDC(request.id) { log.error("failed to write packet to socket", err) }
          result.fail(err)
        }
        try {
          if (!result.isComplete) {
            result.complete()
          }
        } catch (err: Throwable) {
          asMDC(request.id) { log.error("failed to write completion notification to handler", err) }
          result.fail(err)
        }
      }
    } catch (err: Throwable) {
      asMDC(request.id) { log.error("failed to schedule write operation to context") }
      result.fail(err)
    }
    return result
  }

  /**
   * direct callback from the socket when there's a new [buffer] available
   * the response is validated
   * if it has an requestId with an assigned strategy, it's dispatched for processing by the respective [InvocationStrategy]
   * otherwise it is logged as an error
   */
  private fun socketHandler(buffer: Buffer) {
    val jo = JsonObject(buffer)
    if (!jo.containsKey("id")) {
      log.warn("received message without 'id' field from ${config.serviceURI}")
      return
    }
    val responseId = jo.getLong("id")
    try {
      when {
        responseId == null -> log.error("received response without id {}", buffer.toString())
        !invocations.containsKey(responseId) -> asMDC(responseId) { log.error("no subscriber found for response id {}", responseId) }
        else -> asMDC(responseId) { handlePayload(responseId, jo) }
      }
    } catch (err: Throwable) {
      log.error("failed to handle response message", err)
    }
  }

  /**
   * given a [payload] for a [requestId] finds the respective [InvocationStrategy] for dispatch
   * otherwise, logs error and continues
   */
  private fun handlePayload(requestId: Long, payload: JsonObject) {
    asMDC(requestId) {
      if (log.isTraceEnabled) {
        log.trace("handling response {}", Json.encode(payload))
      }

      try {
        invocations[requestId]?.handlePayload(requestId, payload)
          ?: log.error("no subscriber found for request id $requestId")
      } catch (err: Throwable) {
        log.error("failed to handle response message", err)
      }
    }
  }
}

/**
 * syntactic sugar to set a kotlin function as the close handler of a [WebSocket]
 */
private fun WebSocket.closeHandler(fn: () -> Unit) {
  this.closeHandler {
    fn()
  }
}
