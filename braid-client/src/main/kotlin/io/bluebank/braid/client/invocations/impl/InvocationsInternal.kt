package io.bluebank.braid.client.invocations.impl

import io.bluebank.braid.client.invocations.Invocations
import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.vertx.core.Future

/**
 * Internal interface for [InvocationsImpl] used by all [InvocationStrategy] concrete classes
 */
internal interface InvocationsInternal : Invocations {
  fun nextRequestId(): Long

  /**
   * set the invocation [strategy] for a [requestId]
   */
  fun setStrategy(requestId: Long, strategy: InvocationStrategy<*>)

  /**
   * unset / remove the invocation strategy assigned to [requestId]
   */
  fun removeStrategy(requestId: Long)

  /**
   * writes a [JsonRPCRequest] [request] on the socket to the server
   * @returns future to indicate if the send was successful or not
   */
  fun send(request: JsonRPCRequest): Future<Unit>
}