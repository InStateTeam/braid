package io.bluebank.braid.core.jsonrpc

import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.vertx.core.json.Json
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class ParameterTests {
  data class Receiver(val params: Any)
  data class SenderArray(val params: List<Int>)
  data class SenderObject(val params: Params)
  data class Params(val name: String, val age: Int)

  @Before
  fun before() {
    with(KotlinModule()) {
      Json.mapper.registerModule(this)
      Json.prettyMapper.registerModule(this)
    }
  }

  @Test
  fun deserialiseList() {
    val encoded = Json.encode(SenderArray(listOf(1, 2, 3)))
    val decoded = Json.decodeValue(encoded, Receiver::class.java)
    assertTrue(decoded.params is List<*>)
  }
  @Test
  fun deserialiseMap() {
    val encoded = Json.encode(SenderObject(Params("Fred", 40)))
    val decoded = Json.decodeValue(encoded, Receiver::class.java)
    assertTrue(decoded.params is Map<*, *>)
  }
}
