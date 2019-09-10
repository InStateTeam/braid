package io.bluebank.braid.corda.rest.docs.v3

import io.swagger.core.v3.models.Swagger
import org.hamcrest.CoreMatchers
import org.junit.Assert.*
import org.junit.Test

class SwaggerV3Test{
  @Test
  fun `should generate types`() {
     Swagger()

    assertThat(,CoreMatchers.is())
  }


  data class Foo (val bar: Bar)
  data class Bar(val name: String)
}