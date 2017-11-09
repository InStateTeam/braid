package io.bluebank.hermes.core.jsonrpc

import io.bluebank.hermes.core.json.HermesJacksonInit
import io.vertx.core.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JsonRPCRequestTest {
  @Before
  fun before() {
    HermesJacksonInit.init()
  }

  @Test
  fun testDeser() {
    val str = """{"jsonrpc":"2.0","method":"add","params":1,"id":-9007199254740991}"""
    val result = Json.decodeValue(str, JsonRPCRequest::class.java)
    assertEquals(-9007199254740991, result.id)
  }
}
