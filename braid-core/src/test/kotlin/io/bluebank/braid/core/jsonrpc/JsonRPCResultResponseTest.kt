package io.bluebank.braid.core.jsonrpc

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.bluebank.braid.core.json.BraidJacksonInit
import io.vertx.core.json.Json
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class JsonRPCResultResponseTest {
  @Before
  fun before() {
    BraidJacksonInit.init()
  }

  @Test
  fun shouldBeAbleToPreserveTypeInformationDuringSerialisation() {
    val expected = "{\"result\":{\"type\":\"MeteringModelData\",\"someString\":\"wobble\"},\"id\":1,\"jsonrpc\":\"2.0\"}"
    val jsonResponse = JsonRPCResultResponse(id = 1, result = MeteringModelData("wobble"))
    Assert.assertEquals(expected, Json.encode(jsonResponse))
  }

  @JsonTypeInfo(
      use = JsonTypeInfo.Id.NAME,
      include = JsonTypeInfo.As.PROPERTY,
      property = "type"
  )
  @JsonSubTypes(
      JsonSubTypes.Type(value = MeteringModelData::class, name = "MeteringModelData")
  )
  interface ModelData

  data class MeteringModelData(val someString: String) : ModelData

}