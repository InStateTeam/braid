package io.bluebank.braid.corda.swagger.v3

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.*
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.jackson.ModelResolver
import io.vertx.core.json.Json
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasKey
import org.junit.Test


class JacksonPolymorphismTest {

  @Test
  fun `should generate polymorphic swagger based on json annotations`() {

    val schemas = ModelConverters()
        .apply {
          addConverter(ModelResolver(Json.mapper))
        }
        .readAll(Pet::class.java)
    assertThat(schemas, hasKey("Dog"))

    Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    println(Json.encodePrettily(schemas))
  }

  @Test
  fun `should serialize Dog`() {
    val dog = Dog(true, 4)
    val json = Json.encode(dog)

    val roundTripDog = Json.decodeValue(json, Pet::class.java)
    assertThat("expecting dog", roundTripDog, `is`(dog as Pet))
  }

  @Test
  fun `should round trip Dog`() {
    val dog = Dog(true, 4)
    val json = Json.encode(dog)

    assertThat(json, equalTo("""{"type":"Dog","noisy":true,"legs":4}"""))
  }


  @JsonTypeInfo(use = CLASS, include = PROPERTY, property = "@class")
 // @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
  @JsonSubTypes(
      Type(value = Dog::class, name = "Dog"),
      Type(value = Cat::class, name = "Cat")
  )
  abstract class Pet {
  }

  data class Dog(val noisy: Boolean = false, val legs: Int = 4) : Pet() {
  }

  data class Cat(val friendly: Boolean = true, val legs: Int = 4) : Pet() {
  }

}