package io.bluebank.braid.core.json

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.json.Json

class BraidJacksonInit {
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
