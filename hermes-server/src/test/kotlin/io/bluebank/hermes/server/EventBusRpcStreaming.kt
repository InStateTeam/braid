package io.bluebank.hermes.server

import io.bluebank.hermes.core.jsonrpc.JsonRPCError
import io.bluebank.hermes.core.jsonrpc.JsonRPCErrorResponse
import io.bluebank.hermes.core.jsonrpc.JsonRPCRequest
import io.bluebank.hermes.core.jsonrpc.JsonRPCResultResponse
import io.bluebank.hermes.core.logging.loggerFor
import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus
import io.vertx.core.eventbus.MessageCodec
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject

class EventBusRpcStreaming : AbstractVerticle() {
  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      Vertx.vertx().deployVerticle(EventBusRpcStreaming())
    }

    private val logger = loggerFor<EventBusRpcStreaming>()
  }

  override fun start() {
    vertx.eventBus().registerDefaultCodec(JsonRPCRequest::class.java, JsonCodec(JsonRPCRequest::class.java))
    vertx.eventBus().registerDefaultCodec(JsonRPCResultResponse::class.java, JsonCodec(JsonRPCResultResponse::class.java))
    vertx.eventBus().consumer<JsonRPCRequest>("topic") {
      val response = JsonRPCResultResponse(result = 5, id = it.body().id)
      it.reply(response)
    }
    vertx.eventBus().publisher<JsonRPCRequest>("topic").send<JsonRPCResultResponse>(JsonRPCRequest(id = 1, method = "hello", params = null)) {
      logger.info(Json.encode(it.result().body()))
    }
  }
}


data class JsonRPCRequestWithResponseAddress(val request: JsonRPCRequest, val address: String)

class ResponseObserver(val eventbus: EventBus, val request: JsonRPCRequestWithResponseAddress) {
  fun onNext(t: Any?) {
    publish(JsonRPCResultResponse(result = t, id = request.request.id))
  }

  fun onCompleted() {
    publish(JsonRPCCompleted(id = request.request.id))
  }

  fun onError(error: JsonRPCError) {
    publish(JsonRPCErrorResponse(error, id = request.request.id))
  }

  private fun publish(value: Any) {
    eventbus.publish(request.address, JsonObject(Json.encode(value)))
  }

  private data class JsonRPCCompleted(val id : Any? = null)
}

class JsonCodec<T>(val clazz: Class<T>) : MessageCodec<T, T> {
  override fun transform(s: T): T {
    return s
  }

  override fun encodeToWire(buffer: Buffer, s: T) {
    buffer.appendBuffer(Json.encodeToBuffer(s))
  }

  override fun decodeFromWire(pos: Int, buffer: Buffer): T {
    val length = buffer.getInt(pos)
    val newPos = pos + 4
    val msgBuffer = buffer.getBuffer(newPos, newPos + length)
    return Json.decodeValue(msgBuffer, clazz)
  }

  override fun systemCodecID(): Byte {
    return -1
  }

  override fun name(): String {
    return "json encoder for type ${clazz.canonicalName}"
  }
}