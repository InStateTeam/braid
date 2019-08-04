package io.bluebank.braid.corda.rest.docs

import io.swagger.annotations.ApiParam
import io.swagger.models.parameters.QueryParameter
import io.vertx.core.http.HttpMethod
import net.corda.core.node.services.vault.DEFAULT_PAGE_SIZE
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.ws.rs.QueryParam
import kotlin.reflect.jvm.javaType

class KEndPointTest {
  @Test
  fun `that names can be applied with @QueryParam`() {
    val method = this::listAccountsPagedRest
    val endPoint = KEndPoint(
      "",
      false,
      HttpMethod.GET,
      "/foo/",
      "my-name",
      method.parameters,
      method.returnType.javaType,
      method.annotations
    )
    val operation = endPoint.toOperation()
    assertEquals(2, operation.parameters.size)
    val pnq = operation.parameters.single { it.name  == "page-number" && it is QueryParameter} as QueryParameter
    val psq = operation.parameters.single { it.name  == "page-size" && it is QueryParameter} as QueryParameter
    assertEquals(1, pnq.defaultValue)
    assertEquals(DEFAULT_PAGE_SIZE, psq.defaultValue)
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
}