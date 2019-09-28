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
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import io.bluebank.braid.corda.BraidCordaJacksonSwaggerInit
import io.swagger.v3.oas.models.OpenAPI
import io.vertx.core.http.HttpMethod
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

class DocsHandlerV3Test {
  companion object {
    @BeforeClass
    @JvmStatic
    fun beforeClass() {
      BraidCordaJacksonSwaggerInit.init()
    }
  }

  private val openApi: OpenAPI

  init {
    val docs = DocsHandlerV3()
    docs.add("group", false, HttpMethod.POST, "path", this::myFunction)
    openApi = docs.createSwagger()
  }

  fun myFunction(type: aType) {
  }

  data class aType(
      val requiredString: String,
      val requiredDefaultString: String = "",
      val optionalString: String?
  )


  @Test
  fun `should generate openApi`() {
    val path = openApi.paths["path"]
    assertThat(path, notNullValue())
  }


  @Test
  fun `should mark requiredString as required`() {
    val schema =  openApi.components.schemas["io.bluebank.braid.corda.rest.docs.v3.DocsHandlerV3Test_aType"]
    assertThat(schema?.required, hasItem("requiredString"))
  }

  @Test
  @Ignore  // todo does not figure out defauled values on kotlin
  fun `should mark requiredDefaultString as required`() {
    val schema =  openApi.components.schemas["io.bluebank.braid.corda.rest.docs.v3.DocsHandlerV3Test_aType"]
    assertThat(schema?.required, not(hasItem("requiredDefaultString")))
  }

 @Test
  fun `should mark optionalString as not required`() {
    val schema =  openApi.components.schemas["io.bluebank.braid.corda.rest.docs.v3.DocsHandlerV3Test_aType"]
    assertThat(schema?.required, not(hasItem("optionalString")))
  }


  @Test
  fun `should Print the swagger`() {
    val swagger = ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(openApi);

    println(swagger)
  }
}
