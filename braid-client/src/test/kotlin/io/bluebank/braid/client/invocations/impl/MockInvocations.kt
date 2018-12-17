package io.bluebank.braid.client.invocations.impl

import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.jsonrpc.JsonRPCResponse
import io.vertx.core.Future
import io.vertx.core.json.Json

internal class MockInvocations(private val writeCallback: (JsonRPCRequest) -> Future<Unit>) : InvocationsInternalImpl() {
  /**
   * send a request to the network
   */
  override fun send(request: JsonRPCRequest): Future<Unit> {
    return writeCallback(request)
  }

  fun receive(response: JsonRPCResponse) {
    receive(Json.encodeToBuffer(response))
  }
}