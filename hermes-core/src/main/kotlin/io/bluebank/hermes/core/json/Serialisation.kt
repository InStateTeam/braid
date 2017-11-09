package io.bluebank.hermes.core.json

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.json.Json

class HermesJacksonInit {
  companion object {
    init {
      with(KotlinModule()) {
        Json.mapper.registerModule(this)
        Json.prettyMapper.registerModule(this)
      }
    }
    fun init() {
      // automatically init during class load
    }
  }
}
