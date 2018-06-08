package io.bluebank.braid.corda.restafarian.docs

import io.swagger.models.Operation
import io.swagger.models.parameters.Parameter
import io.vertx.core.http.HttpMethod

abstract class EndPoint(val groupName: String, val method: HttpMethod, val path: String) {
  abstract val description: String
  abstract fun decorateOperationWithResponseType(operation: Operation)
  abstract fun toSwaggerParams(): List<Parameter>
}