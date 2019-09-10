package io.bluebank.braid.corda.rest.docs.v3

import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.OpenAPI
import io.vertx.core.http.HttpMethod
//import io.swagger.v3.oas.models.OpenAPI
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Assert.*
import org.junit.Test

class OpenAPITest{
  @Test
  fun `should generate types`() {
    val schema = ModelConverters.getInstance().readAllAsResolvedSchema(Foo::class.java)
    
    assertThat(schema.schema.name,equalTo("Foo"))
    assertThat(schema.referencedSchemas.get("Bar"), notNullValue())
  }

 @Test
  fun `should generate binary properties`() {
    val schema = ModelConverters.getInstance().readAllAsResolvedSchema(Foo::class.java)

    assertThat(schema.schema.name,equalTo("Foo"))
    assertThat(schema.referencedSchemas.get("Bar"), notNullValue())
  }



  data class Foo (val bar: Bar)
  data class Bar(val name: String)
}