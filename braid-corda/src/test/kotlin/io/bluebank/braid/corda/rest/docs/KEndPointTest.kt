package io.bluebank.braid.corda.rest.docs

import io.bluebank.braid.core.annotation.MethodDescription
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.models.parameters.PathParameter
import io.swagger.models.parameters.QueryParameter
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpMethod.GET
import io.vertx.core.http.HttpMethod.POST
import net.corda.core.node.services.vault.DEFAULT_PAGE_SIZE
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import kotlin.reflect.KFunction

class KEndPointTest {
  @Test
  fun `that names can be applied with @QueryParam`() {
    val method = this::listAccountsPagedRest
    val operation = createOperation(handler = method)
    assertEquals(2, operation.parameters.size)
    val pnq =
      operation.parameters.single { it.name == "page-number" && it is QueryParameter } as QueryParameter
    val psq =
      operation.parameters.single { it.name == "page-size" && it is QueryParameter } as QueryParameter
    assertEquals(1, pnq.defaultValue)
    assertEquals(DEFAULT_PAGE_SIZE, psq.defaultValue)
  }

  @Test
  fun `that multiple description annotations prefers method description`() {
    val method = this::manyDescription
    val operation = createOperation(handler = method)
    assertEquals("method-description", operation.description)
  }

  @Test
  fun `that @MethodDescription description is used`() {
    val method = this::methodDescription
    val operation = createOperation(handler = method)
    assertEquals("method-description", operation.description)
  }

  @Test
  fun `that @ApiParameter description is used`() {
    val method = this::apiParamDescription
    val operation = createOperation(handler = method)
    assertEquals("api-operation", operation.description)
  }

  @Test
  fun `that @PathParms are represented correctly`() {
    val method = this::postWithPathParam
    val operation = createOperation(method = POST, path = "/foo/:name", handler = method)
    assertEquals(1, operation.parameters.size)
    val p = operation.parameters.filterIsInstance<PathParameter>().single()
    assertEquals("name", p.name)
    assertEquals("default-path-param", p.defaultValue)
  }

  @Test
  fun `that all parameter types can be mixed for post`() {
    val method = this::postWithPathQueryAndBodyParam
    val operation = createOperation(method = POST, path = "/foo/:name", handler = method)
    assertEquals(3, operation.parameters.size)
    val p = operation.parameters.filterIsInstance<PathParameter>().single()
    assertEquals("name", p.name)
    assertEquals("default-path-param", p.defaultValue)
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
  ) {
  }

  @MethodDescription(description = "method-description")
  @ApiOperation(value = "api-operation")
  fun manyDescription() {
  }

  @MethodDescription(description = "method-description")
  fun methodDescription() {
  }

  @ApiOperation(value = "api-operation")
  fun apiParamDescription() {
  }

  @ApiOperation(value = "post with path param")
  fun postWithPathParam(
    @ApiParam(defaultValue = "default-path-param")
    @PathParam("name")
    name: String
  ) {
  }

  @ApiOperation(value = "post with path param query param and body")
  fun postWithPathQueryAndBodyParam(
    @ApiParam(defaultValue = "default-path-param")
    @PathParam("name")
    name: String,
    @ApiParam(defaultValue = "default-path-param")
    @QueryParam("age")
    age: Int,
    @ApiParam(defaultValue = "details")
    details: String
  ) {}

  private fun <R> createOperation(
    method: HttpMethod = GET,
    path: String = "/foo",
    group: String = "",
    name: String = "my-name",
    protected: Boolean = false,
    handler: KFunction<R>
  ) = EndPoint.create(
    group,
    protected,
    method,
    path,
    "my-name",
    handler.parameters,
    handler.returnType,
    handler.annotations
  ).toOperation()

}