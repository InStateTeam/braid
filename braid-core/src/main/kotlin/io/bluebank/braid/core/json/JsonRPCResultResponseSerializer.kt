package io.bluebank.braid.core.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.bluebank.braid.core.jsonrpc.JsonRPCResultResponse

class JsonRPCResultResponseSerializer : StdSerializer<JsonRPCResultResponse>(JsonRPCResultResponse::class.java) {
  override fun serialize(value: JsonRPCResultResponse, generator: JsonGenerator, provider: SerializerProvider) {
    generator.writeStartObject()
    generator.writeObjectField("result", value.result)
    if (value.id !== null) {
      generator.writeObjectField("id", value.id)
    }
    generator.writeStringField("jsonrpc", value.jsonrpc)

    generator.writeEndObject()
  }
}