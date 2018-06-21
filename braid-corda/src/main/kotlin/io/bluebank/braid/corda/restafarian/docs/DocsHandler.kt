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
package io.bluebank.braid.corda.restafarian.docs

import io.bluebank.braid.corda.restafarian.toSwaggerPath
import io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON
import io.netty.handler.codec.http.HttpResponseStatus
import io.swagger.models.*
import io.swagger.models.auth.SecuritySchemeDefinition
import io.swagger.util.Json
import io.vertx.core.Handler
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpMethod.*
import io.vertx.ext.web.RoutingContext
import java.net.URL
import kotlin.reflect.KCallable

class DocsHandler(
  private val serviceName: String = "",
  private val description: String = "",
  private val basePath: String = "http://localhost:8080",
  private val scheme: Scheme = Scheme.HTTPS,
  private val contact: Contact = Contact()
    .name("")
    .email("")
    .url(""),
  private val auth: SecuritySchemeDefinition? = null
) : Handler<RoutingContext> {
  companion object {
    internal const val SECURITY_DEFINITION_NAME = "Authorization"
  }

  private var currentGroupName: String = ""
  private val endpoints = mutableListOf<EndPoint>()
  private val models = mutableMapOf<String, Model>()
  private val swagger: Swagger by lazy {
    createSwagger()
  }

  override fun handle(context: RoutingContext) {
    val absoluteURI = URL(context.request().absoluteURI())
    swagger.host = absoluteURI.authority
    val output = Json.pretty().writeValueAsString(swagger)
    context.response()
      .setStatusCode(HttpResponseStatus.OK.code())
      .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
      .putHeader(HttpHeaders.CONTENT_LENGTH, output.length.toString())
      .end(output)
  }

  private fun createSwagger(): Swagger {
    val url = URL(basePath)
    val info = createSwaggerInfo()
    return Swagger()
      .info(info)
      .host(url.host + ":" + url.port)
      .basePath(url.path)
      .apply {
        if (auth != null) {
          securityDefinition(SECURITY_DEFINITION_NAME, auth)
        }
      }
      .scheme(scheme)
      .consumes(APPLICATION_JSON.toString())
      .produces(APPLICATION_JSON.toString())
      .apply {
        addAllModels(models)
        endpoints.forEach {
          addEndpoint(it)
        }
      }
  }

  private fun createSwaggerInfo(): Info? {
    val info = Info()
      .version("1.0.0")
      .title(serviceName)
    info.description = description
    info.contact = contact
    return info
  }

  private fun Swagger.addEndpoint(endpoint: EndPoint) {

    val swaggerPath = endpoint.path.toSwaggerPath()
    val path = if (this.paths != null && this.paths.contains(swaggerPath)) {
      paths[swaggerPath]!!
    } else {
      val path = Path()
      this.path(swaggerPath, path)
      path
    }

    val operation = endpoint.toOperation()

    when (endpoint.method) {
      GET -> path.get(operation)
      POST -> path.post(operation)
      PUT -> path.put(operation)
      DELETE -> path.delete(operation)
      PATCH -> path.patch(operation)
      OPTIONS, HEAD, TRACE, CONNECT, OTHER -> TODO("Implement ${endpoint.method.name}")
    }
  }

  fun <Response> add(groupName: String, protected: Boolean, method: HttpMethod, path: String, handler: KCallable<Response>) {
    val endpoint = EndPoint.create(groupName, protected, method, path, handler.name, handler.parameters, handler.returnType, handler.annotations)
    add(endpoint)
  }

  fun add(groupName: String, protected: Boolean, method: HttpMethod, path: String, handler: (RoutingContext) -> Unit) {
    val endpoint = EndPoint.create(groupName, protected, method, path, handler)
    add(endpoint)
  }

  private fun add(endpoint: EndPoint) {
    endpoints.add(endpoint)
    endpoint.addTypes(models)
  }


  private fun Swagger.addAllModels(types: Map<String, Model>): Swagger {
    types.forEach { name, model ->
      this.model(name, model)
    }
    return this
  }

  fun group(groupId: String, fn: () -> Unit) {
    this.currentGroupName = groupId
    fn()
  }
}

