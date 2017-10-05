package io.bluebank.jsonrpc

import io.bluebank.jsonrpc.server.JacksonKotlinInit
import io.bluebank.jsonrpc.server.JsonRPCRequest
import io.vertx.core.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class JsonRPCRequestTest {
  @Before
  fun before() {
    JacksonKotlinInit.init()
  }

  @Test
  fun testDeser() {
    val str = """{"jsonrpc":"2.0","method":"add","params":1,"id":-9007199254740991}"""
    val result = Json.decodeValue(str, JsonRPCRequest::class.java)
    assertEquals(-9007199254740991, result.id)
  }
}
