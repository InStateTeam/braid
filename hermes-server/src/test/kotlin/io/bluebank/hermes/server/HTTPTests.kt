package io.bluebank.hermes.server

import io.vertx.core.json.Json
import org.junit.Test
import kotlin.test.assertEquals

class HTTPTests {
  @Test
  fun `that we can string jsonify`() {
    val result = Json.encode("hello")
    assertEquals("\"hello\"", result)
  }
}