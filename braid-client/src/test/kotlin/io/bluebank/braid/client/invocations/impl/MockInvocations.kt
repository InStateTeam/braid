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