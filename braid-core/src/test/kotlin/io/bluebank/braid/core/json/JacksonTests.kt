package io.bluebank.braid.core.json

import io.vertx.core.json.Json
import org.junit.Before
import org.junit.Test
import kotlin.reflect.jvm.javaType

class JacksonTests {
  @Before
  fun before() = BraidJacksonInit.init()

  @Test
  fun test() {
    val apples = getApples()
    val encoded = Json.encodePrettily(apples)
    val returnType = ::getApples.returnType.javaType
    val jacksonReturnType = Json.mapper.constructType(returnType)
    val result = Json.mapper.readValue<Any>(encoded, jacksonReturnType)
  }
}

data class Apple(val colour: String)
fun getApples() : List<Apple> {
  return listOf(Apple("red"), Apple("yellow"))
}