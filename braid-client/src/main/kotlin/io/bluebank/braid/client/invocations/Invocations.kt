package io.bluebank.braid.client.invocations

import io.bluebank.braid.client.BraidClientConfig
import io.bluebank.braid.client.invocations.impl.InvocationsImpl
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientOptions
import java.io.Closeable
import java.lang.reflect.Type

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
interface Invocations : Closeable {
  companion object {
    private val log = loggerFor<Invocations>()
    private val defaultClientHttpOptions = HttpClientOptions()

    fun defaultSocketCloseHandler() = {
      log.info("closing...")
    }

    fun defaultSocketExceptionHandler() = { error: Throwable ->
      // this implementation is nominal for logging the socket error
      // TODO: consider connection retry
      log.error("exception from socket", error)
    }

    fun create(
      vertx: Vertx,
      config: BraidClientConfig,
      exceptionHandler: (Throwable) -> Unit = defaultSocketExceptionHandler(),
      closeHandler: () -> Unit = defaultSocketCloseHandler(),
      clientOptions: HttpClientOptions = defaultClientHttpOptions
    ): Invocations {
      return InvocationsImpl(vertx, config, exceptionHandler, closeHandler, clientOptions)
    }
  }

  val activeRequestsCount: Int

  /**
   * public entry point to invoke a method. may block depending if the call has synchronous signature
   * may not invoke anything at all if the call returns an [rx.Observable]
   * @param [method] the name of the method
   * @param [returnType] the expected return type of the function being called
   * @param [params] the parameters for the call
   * @return the result of the invocation
   */
  fun invoke(method: String, returnType: Type, params: Array<out Any?>): Any?
}