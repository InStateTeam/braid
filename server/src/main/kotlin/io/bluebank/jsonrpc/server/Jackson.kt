package io.bluebank.jsonrpc.server

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.json.Json

class JacksonKotlinInit {
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
