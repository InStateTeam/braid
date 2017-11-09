package io.bluebank.hermes.corda.serialisation

import com.fasterxml.jackson.databind.module.SimpleModule
import io.bluebank.hermes.core.json.HermesJacksonInit
import io.vertx.core.json.Json
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.utilities.OpaqueBytes
import java.security.PublicKey

class HermesCordaJacksonInit {
  companion object {
    init {
      HermesJacksonInit.init()
      val sm = SimpleModule()
          .addAbstractTypeMapping(AbstractParty::class.java, Party::class.java)
          .addSerializer(PublicKey::class.java, PublicKeySerializer())
          .addDeserializer(PublicKey::class.java, PublicKeyDeserializer())
          .addSerializer(Amount::class.java, AmountSerializer())
          .addDeserializer(Amount::class.java, AmountDeserializer())
          .addSerializer(OpaqueBytes::class.java, OpaqueBytesSerializer())
          .addDeserializer(OpaqueBytes::class.java, OpaqueBytesDeserializer())
          .addSerializer(CordaX500Name::class.java, CordaX500NameSerializer())
          .addDeserializer(CordaX500Name::class.java, CordaX500NameDeserializer())
          .addSerializer(Issued::class.java, IssuedSerializer())
          .addDeserializer(Issued::class.java, IssuedDeserializer())

      Json.mapper.registerModule(sm)
      Json.prettyMapper.registerModule(sm)
    }

    fun init() {
      // automatically initialise the static constructor
    }
  }
}

