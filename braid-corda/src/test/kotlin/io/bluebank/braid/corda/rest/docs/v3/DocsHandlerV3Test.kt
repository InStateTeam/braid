package io.bluebank.braid.corda.rest.docs.v3

//import io.swagger.v3.oas.models.OpenAPI
import com.nhaarman.mockito_kotlin.notNull
import io.vertx.core.http.HttpMethod
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class DocsHandlerV3Test {

  @Test
  fun `should generate openApi`() {

    val docs = DocsHandlerV3()
    docs.add("group", false, HttpMethod.POST, "path", this::myFunction)
    val createSwagger = docs.createSwagger()


    val path = createSwagger.paths.get("path")
    assertThat(path, notNullValue())
  }

  fun myFunction() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
