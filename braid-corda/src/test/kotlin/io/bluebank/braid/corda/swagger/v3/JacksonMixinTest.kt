package io.bluebank.braid.corda.swagger.v3

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.*
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.jackson.ModelResolver
import io.vertx.core.json.Json
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasKey
import org.junit.Test


class JacksonMixinTest {


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

    assertThat(json, equalTo("""{"@class":"io.bluebank.braid.corda.swagger.v3.JacksonMixinTest${'$'}Dog","noisy":true,"legs":4}"""))
  }

  @Test
  fun `should generate polymorphic swagger based on json annotations`() {

    Json.mapper.addMixIn(Pet::class.java, PetMixin::class.java)

    val schemas = ModelConverters()
        .apply {
          addConverter(ModelResolver(Json.mapper))
        }
        .readAll(Pet::class.java)
    // AnnotatedType(Pet::class.java).ctxAnnotations(PetMixin::class.annotations.toTypedArray())
    assertThat(schemas, hasKey("Dog"))

    Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    println(Json.encodePrettily(schemas))
  }


  // @JsonTypeInfo(use = NAME, include = PROPERTY, property = "type")
  @JsonSubTypes(
      Type(value = Dog::class, name = "Dog"),
      Type(value = Cat::class, name = "Cat")
  )
  @JsonTypeInfo(use = CLASS, include = PROPERTY, property = "@class", defaultImpl=Pet::class)
  abstract class PetMixin{
  }


 // @JsonTypeInfo(use = CLASS, include = PROPERTY, property = "@class")
  abstract class Pet {
  }

  data class Dog(val noisy: Boolean = false, val legs: Int = 4) : Pet() {
  }

  data class Cat(val friendly: Boolean = true, val legs: Int = 4) : Pet() {
  }

}