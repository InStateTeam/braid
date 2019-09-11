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
package io.bluebank.braid.corda.rest.docs

import io.bluebank.braid.corda.rest.SwaggerInfo
import io.bluebank.braid.corda.rest.toSwaggerPath
import io.bluebank.braid.core.logging.loggerFor
import io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON
import io.netty.handler.codec.http.HttpResponseStatus
import io.swagger.models.*
import io.swagger.models.auth.SecuritySchemeDefinition
import io.swagger.util.Json
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpMethod.*
import io.vertx.ext.web.RoutingContext
import java.lang.reflect.Type
import java.net.URL
import kotlin.reflect.KCallable

class DocsHandlerV2(
    private val swaggerInfo: SwaggerInfo = SwaggerInfo(),
    private val scheme: Scheme = Scheme.HTTPS,

    private val basePath: String = "http://localhost:8080",
    private val auth: SecuritySchemeDefinition? = null,
    private val debugMode: Boolean = false
) : DocsHandler {

  companion object {
    var log = loggerFor<DocsHandlerV2>()
    internal const val SECURITY_DEFINITION_NAME = "Authorization"
  }

  private var currentGroupName: String = ""
  private val endpoints = mutableListOf<EndPoint>()
  private val swagger: Swagger by lazy {
    createSwagger()
  }
  private val modelContext = ModelContext()

  override fun handle(context: RoutingContext) {
    val absoluteURI = URL(context.request().absoluteURI())
    swagger.host = absoluteURI.authority
    val output =
        Json.pretty().writeValueAsString(if (debugMode) createSwagger() else swagger)
    context.response()
        .setStatusCode(HttpResponseStatus.OK.code())
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .putHeader(HttpHeaders.CONTENT_LENGTH, output.length.toString())
        .end(output)
  }

  override fun swagger(): String {
    return io.swagger.util.Json.pretty().writeValueAsString(createSwagger())
  }

  fun createSwagger(): Swagger {
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
          modelContext.addToSwagger(this)
          endpoints.forEach {
            addEndpoint(it)
          }
        }
  }

  private fun createSwaggerInfo(): Info? {
    val info = Info()
        .version(swaggerInfo.version)
        .title(swaggerInfo.serviceName)
        .description(swaggerInfo.description)
        .contact(Contact()
            .name(swaggerInfo.contact.name)
            .email(swaggerInfo.contact.email)
            .url(swaggerInfo.contact.url))
    return info
  }

  private fun Swagger.addEndpoint(endpoint: EndPoint) {
    try {
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
    } catch (e: Throwable) {
      log.warn("Unable to add endpoint: $endpoint ${e.message}")
    }
  }

  override fun <Response> add(
      groupName: String,
      protected: Boolean,
      method: HttpMethod,
      path: String,
      handler: KCallable<Response>
  ) {
    val endpoint = EndPoint.create(
        groupName,
        protected,
        method,
        path,
        handler.name,
        handler.parameters,
        handler.returnType,
        handler.annotations
    )
    add(endpoint)
  }

  override fun add(
      groupName: String,
      protected: Boolean,
      method: HttpMethod,
      path: String,
      handler: (RoutingContext) -> Unit
  ) {
    val endpoint = EndPoint.create(groupName, protected, method, path, handler)
    add(endpoint)
  }

  private fun add(endpoint: EndPoint) {
    endpoints.add(endpoint)
    endpoint.addTypes(modelContext)
  }

  fun group(groupId: String, fn: () -> Unit) {
    this.currentGroupName = groupId
    fn()
  }

  override fun addType(type: Type) {
    modelContext.addType(type)
  }
}

