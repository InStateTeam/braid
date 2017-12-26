package io.bluebank.braid.core.jsonrpc

import io.bluebank.braid.core.json.BraidJacksonInit
import io.vertx.core.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JsonRPCRequestTest {
  @Before
  fun before() {
    BraidJacksonInit.init()
  }

  @Test
  fun testDeser() {
    val str = """{"jsonrpc":"2.0","method":"add","params":1,"id":-9007199254740991}"""
    val result = Json.decodeValue(str, JsonRPCRequest::class.java)
    assertEquals(-9007199254740991, result.id)
  }
}
