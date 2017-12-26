package io.bluebank.braid.corda.serialisation;

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.parseAsHex
import net.corda.core.utilities.toHexString

class OpaqueBytesSerializer : StdSerializer<OpaqueBytes>(OpaqueBytes::class.java) {
  override fun serialize(value: OpaqueBytes, generator: JsonGenerator, provider: SerializerProvider) {
    generator.writeString(value.bytes.copyOfRange(value.offset, value.offset + value.size).toHexString())
  }
}

class OpaqueBytesDeserializer : StdDeserializer<OpaqueBytes>(OpaqueBytes::class.java) {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): OpaqueBytes {
    return try {
      // try converting as hex
      OpaqueBytes(parser.text.parseAsHex())
    } catch (err: IllegalArgumentException) {
      // convert the string to bytes
      OpaqueBytes(parser.text.toByteArray())
    }
  }
}