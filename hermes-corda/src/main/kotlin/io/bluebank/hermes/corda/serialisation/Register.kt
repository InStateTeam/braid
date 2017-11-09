package io.bluebank.hermes.corda.serialisation

import com.fasterxml.jackson.databind.module.SimpleModule
import io.vertx.core.json.Json
import java.security.PublicKey
import java.util.concurrent.atomic.AtomicBoolean

private val _registered = AtomicBoolean()

fun registerHermesJacksonSerializers() {
  if (_registered.compareAndSet(false, true)) {
    val sm = SimpleModule()
    sm.addSerializer(PublicKey::class.java, PublicKeySerializer())
    sm.addDeserializer(PublicKey::class.java, PublicKeyDeserializer())
    Json.mapper.registerModule(sm)
    Json.prettyMapper.registerModule(sm)
  }
}
