/**
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bluebank.braid.corda.rest.docs.v3

//import io.swagger.v3.oas.models.OpenAPI
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.type.TypeFactory
import io.bluebank.braid.corda.rest.TestService
import io.swagger.util.Json
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Assert.assertThat
import org.junit.Test
import kotlin.reflect.jvm.javaType

class OpenAPITest{
  @Test
  fun `should generate types`() {
    val schema = ModelConverters.getInstance().readAllAsResolvedSchema(Foo::class.java)

    assertThat(schema.schema.name,equalTo("Foo"))
    assertThat(schema.referencedSchemas["Bar"], notNullValue())
  }

  @Test
  fun `should generate binary properties`() {
    val schema = ModelConverters.getInstance().readAllAsResolvedSchema(Foo::class.java)

    assertThat(schema.schema.name,equalTo("Foo"))
    assertThat(schema.referencedSchemas.get("Bar"), notNullValue())
  }

  @Test
  fun `see how Reader works`() {
    val cls = String::class.java
    val type = TypeFactory.defaultInstance().constructType(cls);
    val bd = Json.mapper().serializationConfig.introspect<BeanDescription>(type)
    val service = TestService()
    val method = service::headerListOfStrings
    method.parameters.map { param ->
      val type = param.type.javaType
      val result = ModelConverters.getInstance().resolveAsResolvedSchema(AnnotatedType(type).resolveAsRef(true))
      println(result)
    }
  }

  data class Foo (val bar: Bar)
  data class Bar(val name: String)
}