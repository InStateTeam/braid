package io.bluebank.braid.corda.rest.docs.v3

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.vertx.core.http.HttpMethod
import kotlin.reflect.KCallable


fun <Response> OpenAPI.path(groupName: String,
                 protected: Boolean,
                 method: HttpMethod,
                 path: String,
                 handler: KCallable<Response>): OpenAPI {

 // path(path, PathItem().method(method, operation()))

  return this;
}

fun PathItem.method(method:HttpMethod, operation: Operation):PathItem{
  when(method){
    HttpMethod.GET-> get(operation)
    HttpMethod.OPTIONS -> options(operation)
    HttpMethod.HEAD -> head(operation)
    HttpMethod.POST -> post(operation)
    HttpMethod.PUT -> put(operation)
    HttpMethod.DELETE -> delete(operation)
    HttpMethod.TRACE -> trace(operation)
    HttpMethod.CONNECT -> TODO("Connect not supported")
    HttpMethod.PATCH -> patch(operation)
    HttpMethod.OTHER -> TODO("Other not supported")
  }
  return this;
}

