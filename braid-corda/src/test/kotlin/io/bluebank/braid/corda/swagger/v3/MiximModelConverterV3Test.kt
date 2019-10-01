package io.bluebank.braid.corda.swagger.v3

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id.*
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import io.swagger.v3.oas.models.media.Schema
import io.vertx.core.json.Json
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.hasKey
import org.junit.Before
import org.junit.Test


class MiximModelConverterV3Test {
   var schemas: MutableMap<String, Schema<Any>>? = null

  @Before
  fun setUp() {
    Json.mapper.addMixIn(Pet::class.java, PetMixin::class.java)

     schemas = ModelConverters()
        .apply {
          addConverter(ModelResolver(Json.mapper))
          addConverter(MiximModelConverterV3(Json.mapper))
        }
        .readAll(Pet::class.java)

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

    assertThat(json, equalTo("""{"@class":".MiximModelConverterV3Test${'$'}Dog","noisy":true,"legs":4}"""))
  }

  @Test
  fun `should generate subtypes`() {
    assertThat(schemas, hasKey("Dog"))
    assertThat(schemas, hasKey("Cat"))
  }

  @Test
  fun `should generate Base type`() {
    assertThat(schemas, hasKey("Pet"))
  }

  @Test
  fun `should exclude mixin`() {
    assertThat(schemas, not(hasKey("PetMixin")))
  }

  @Test
  fun `should generate discriminator on Pet`() {


    // AnnotatedType(Pet::class.java).ctxAnnotations(PetMixin::class.annotations.toTypedArray())

    assertThat(schemas, hasKey("Pet"))
    assertThat(schemas?.get("Pet")?.discriminator, notNullValue())
  }


  @JsonSubTypes(
      Type(value = Dog::class, name = "Dog"),
      Type(value = Cat::class, name = "Cat")
  )
  @JsonTypeInfo(use = MINIMAL_CLASS, include = PROPERTY, property = "@class", defaultImpl=Pet::class)
  @io.swagger.v3.oas.annotations.media.Schema(
      type = "object",
      title = "Pet",
      discriminatorProperty = "@class",
      discriminatorMapping = [
        DiscriminatorMapping(value = ".MiximModelConverterV3Test${'$'}Dog", schema = Dog::class),
        DiscriminatorMapping(value = ".MiximModelConverterV3Test${'$'}Cat", schema = Cat::class)
      ],
      subTypes = [Dog::class, Cat::class]
  )
  abstract class PetMixin{
  }

  abstract class Pet {
  }

  data class Dog(val noisy: Boolean = false, val legs: Int = 4) : Pet() {
  }

  data class Cat(val friendly: Boolean = true, val legs: Int = 4) : Pet() {
  }

}