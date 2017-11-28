package io.bluebank.hermes.corda.serialisation

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import net.corda.client.jackson.JacksonSupport
import net.corda.core.transactions.WireTransaction

class WireTransactionSerializer : StdSerializer<WireTransaction>(WireTransaction::class.java) {
  override fun serialize(value: WireTransaction, gen: JsonGenerator, provider: SerializerProvider) {
    value.id
  }
}