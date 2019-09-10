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


    val path = openApi.paths.get("path")
    println(Json.encodePrettily(openApi))
    assertThat(path, notNullValue())
  }

  fun myFunction() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
