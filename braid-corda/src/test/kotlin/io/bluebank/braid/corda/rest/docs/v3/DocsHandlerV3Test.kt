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
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class DocsHandlerV3Test {

  @Test
  fun `should generate openApi`() {

    val docs = DocsHandlerV3()
    docs.add("group", false, HttpMethod.POST, "path", this::myFunction)
    val openApi = docs.createSwagger()

    val path = openApi.paths["path"]
    println(Json.encodePrettily(openApi))
    assertThat(path, notNullValue())
  }

  fun myFunction() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
