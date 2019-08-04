package io.bluebank.braid.corda.rest.docs

import io.bluebank.braid.core.annotation.MethodDescription
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.models.parameters.QueryParameter
import io.vertx.core.http.HttpMethod
import net.corda.core.node.services.vault.DEFAULT_PAGE_SIZE
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.ws.rs.QueryParam
import kotlin.reflect.KFunction

class KEndPointTest {
  @Test
  fun `that names can be applied with @QueryParam`() {
    val method = this::listAccountsPagedRest
    val operation = createOperation(method)
    assertEquals(2, operation.parameters.size)
    val pnq = operation.parameters.single { it.name  == "page-number" && it is QueryParameter} as QueryParameter
    val psq = operation.parameters.single { it.name  == "page-size" && it is QueryParameter} as QueryParameter
    assertEquals(1, pnq.defaultValue)
    assertEquals(DEFAULT_PAGE_SIZE, psq.defaultValue)
  }


  @Test
  fun `that multiple description annotations prefers method description`() {
    val method = this::manyDescription
    val operation = createOperation(method)
    assertEquals("method-description", operation.description)
  }

  @Test
  fun `that @MethodDescription description is used`() {
    val method = this::methodDescription
    val operation = createOperation(method)
    assertEquals("method-description", operation.description)
  }

  @Test
  fun `that @ApiParameter description is used`() {
    val method = this::apiParamDescription
    val operation = createOperation(method)
    assertEquals("api-operation", operation.description)
  }

  fun listAccountsPagedRest(
    @Suppress("UNUSED_PARAMETER") @QueryParam("page-number")
    @ApiParam(
      value = "page number - must be greater than zero",
      defaultValue = 1.toString(),
      required = true
    )
    pageNumber: Int,
    @Suppress("UNUSED_PARAMETER") @ApiParam(
      value = "max accounts per page",
      defaultValue = DEFAULT_PAGE_SIZE.toString(),
      required = true
    )
    @QueryParam("page-size") pageSize: Int
  ) {}

  @MethodDescription(description = "method-description")
  @ApiOperation(value = "api-operation")
  fun manyDescription() {}

  @MethodDescription(description = "method-description")
  fun methodDescription() {}

  @ApiOperation(value = "api-operation")
  fun apiParamDescription() {}

  private fun <R>createOperation(method: KFunction<R>) = EndPoint.create(
    "",
    false,
    HttpMethod.GET,
    "/foo",
    "my-name",
    method.parameters,
    method.returnType,
    method.annotations
  ).toOperation()

}