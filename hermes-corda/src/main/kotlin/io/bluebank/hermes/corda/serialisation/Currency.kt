package io.bluebank.hermes.corda.serialisation

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import java.util.*

class CurrencySerializer : StdSerializer<Currency>(Currency::class.java) {
  override fun serialize(value: Currency, generator: JsonGenerator, provider: SerializerProvider) {
    generator.writeString(value.currencyCode)
  }
}

class CurrencyDeserializer : StdDeserializer<Currency>(Currency::class.java) {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): Currency {
    return Currency.getInstance(parser.text)
  }
}