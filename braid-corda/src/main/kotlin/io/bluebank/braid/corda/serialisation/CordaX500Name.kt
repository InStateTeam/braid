package io.bluebank.braid.corda.serialisation

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import net.corda.core.identity.CordaX500Name

class CordaX500NameSerializer : StdSerializer<CordaX500Name>(CordaX500Name::class.java) {
  override fun serialize(value: CordaX500Name, generator: JsonGenerator, provider: SerializerProvider) {
    generator.writeString(value.toString())
  }
}

class CordaX500NameDeserializer : StdDeserializer<CordaX500Name>(CordaX500Name::class.java) {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): CordaX500Name {
    return CordaX500Name.parse(parser.text)
  }
}