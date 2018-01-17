/*
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

package io.bluebank.braid.core.jsonrpc

import io.bluebank.braid.core.jsonrpc.JsonRPCErrorResponse.Companion.serverError
import io.bluebank.braid.core.jsonrpc.JsonRPCErrorResponse.Companion.throwInvalidRequest
import io.bluebank.braid.core.service.MethodDoesNotExist
import io.bluebank.braid.core.service.ServiceExecutor
import io.bluebank.braid.core.socket.Socket
import io.bluebank.braid.core.socket.SocketListener
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import rx.Subscription

class JsonRPCMounter(private val executor: ServiceExecutor) : SocketListener<JsonRPCRequest, JsonRPCResponse> {
  private lateinit var socket: Socket<JsonRPCRequest, JsonRPCResponse>
  private val activeSubscriptions = mutableMapOf<JsonRPCRequest, Subscription>()

  override fun onRegister(socket: Socket<JsonRPCRequest, JsonRPCResponse>) {
    this.socket = socket
  }

  override fun dataHandler(socket: Socket<JsonRPCRequest, JsonRPCResponse>, item: JsonRPCRequest) {
    handleRequest(item)
  }

  override fun endHandler(socket: Socket<JsonRPCRequest, JsonRPCResponse>) {
    activeSubscriptions.forEach { _, subscription -> subscription.unsubscribe() }
    activeSubscriptions.clear()
  }

  class FutureHandler(val callback: (AsyncResult<Any?>) -> Unit) : Handler<AsyncResult<Any?>> {
    override fun handle(event: AsyncResult<Any?>) {
      callback(event)
    }
  }

  private fun handleRequest(request: JsonRPCRequest) {
    try {
      checkVersion(request)
      val subscription = executor.invoke(request).subscribe({ handleDataItem(it, request)}, { err -> handlerError(err, request) }, { handleCompleted(request) })
      activeSubscriptions[request] = subscription
    } catch (err: JsonRPCException) {
      err.response.send()
    }
  }

  private fun handleCompleted(request: JsonRPCRequest) {
    try {
      if (request.streamed) {
        val payload = JsonRPCCompletedResponse(id = request.id)
        socket.write(payload)
      }
    } finally {
      activeSubscriptions.remove(request)
    }
  }

  private fun handlerError(err: Throwable, request: JsonRPCRequest) {
    try {
      if (err is MethodDoesNotExist) {
        JsonRPCErrorResponse.methodNotFound(request.id, "method ${request.method} not implemented").send()
      } else {
        serverError(request.id, err.message).send()
      }
    } finally {
      activeSubscriptions.remove(request)
    }
  }

  private fun handleDataItem(result: Any?, request: JsonRPCRequest) {
    val payload = JsonRPCResultResponse(result = result, id = request.id)
    socket.write(payload)
    if (!request.streamed) {
      activeSubscriptions[request]?.unsubscribe()
      activeSubscriptions.remove(request)
    }
  }

  private fun checkVersion(request: JsonRPCRequest) {
    val message = "braid version must be at least 2.0"
    try {
      val version = request.jsonrpc.toDouble()
      if (version < 2.0) {
        throwInvalidRequest(request.id, message)
      }
    } catch (err: NumberFormatException) {
      throwInvalidRequest(request.id, message)
    }
  }

  private fun JsonRPCErrorResponse.send() {
    socket.write(this)
  }
}