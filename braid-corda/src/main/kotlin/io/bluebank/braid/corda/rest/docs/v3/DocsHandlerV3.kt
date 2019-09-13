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

import io.bluebank.braid.corda.rest.SwaggerInfo
import io.bluebank.braid.corda.rest.docs.DocsHandler
import io.bluebank.braid.corda.rest.toSwaggerPath
import io.bluebank.braid.core.logging.loggerFor
import io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON
import io.netty.handler.codec.http.HttpResponseStatus
import io.swagger.v3.core.util.Json
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpMethod.*
import io.vertx.ext.web.RoutingContext
import java.lang.reflect.Type
import java.net.URL
import kotlin.reflect.KCallable

class DocsHandlerV3(
  private val swaggerInfo: SwaggerInfo = SwaggerInfo(),

  private val auth: SecurityScheme? = null,
  private val basePath: String = "http://localhost:8080",
  private val debugMode: Boolean = false
) : DocsHandler {

  companion object {
    var log = loggerFor<DocsHandlerV3>()
    internal const val SECURITY_DEFINITION_NAME = "Authorization"
  }

  private var currentGroupName: String = ""
  private val endpoints = mutableListOf<EndPointV3>()
  private val swagger: OpenAPI by lazy {
    createSwagger()
  }
  private val modelContext = ModelContextV3()

  override fun handle(context: RoutingContext) {
    val openApi = if (debugMode) createSwagger() else swagger
    openApi.addServersItem(Server().url(context.request().absoluteURI().replace("swagger.json", "")))
    val output = Json.mapper().writeValueAsString(openApi)
    context.response()
        .setStatusCode(HttpResponseStatus.OK.code())
        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
        .putHeader(HttpHeaders.CONTENT_LENGTH, output.length.toString())
        .end(output)
  }

  override fun swagger(): String {
    return Json.pretty().writeValueAsString(createSwagger())
  }

  fun createSwagger(): OpenAPI {
    val url = URL(basePath)
    return OpenAPI()
      .apply {
        info(createSwaggerInfo())
        addServersItem(Server().url(url.toExternalForm()))
        // hopefully under server above .basePath(url.path)
        if (auth != null) {
          getOrCreateComponents().securitySchemes = mapOf(SECURITY_DEFINITION_NAME to auth)
        }
        // may be covered under server? .schema(scheme)
        modelContext.addToSwagger(this)
        endpoints.forEach { addEndpoint(it) }
      }
  }

  private fun createSwaggerInfo() = Info().apply {
    version(swaggerInfo.version)
    title(swaggerInfo.serviceName)
    description(swaggerInfo.description)
    contact(Contact().apply {
      name(swaggerInfo.contact.name)
      email(swaggerInfo.contact.email)
      url(swaggerInfo.contact.url)
    })
  }

  private fun OpenAPI.addEndpoint(endpoint: EndPointV3) {
    try {
      val swaggerPath = endpoint.path.toSwaggerPath()
      val path = if (this.paths != null && this.paths.contains(swaggerPath)) {
        paths[swaggerPath]!!
      } else {
        val path = PathItem()
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
      log.warn("Unable to add endpoint $endpoint : ${e.message}")
    }
  }

  override fun <Response> add(
      groupName: String,
      protected: Boolean,
      method: HttpMethod,
      path: String,
      handler: KCallable<Response>
  ) {
    val endpoint = EndPointV3.create(
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
    val endpoint = EndPointV3.create(groupName, protected, method, path, handler)
    add(endpoint)
  }

  private fun add(endpoint: EndPointV3) {
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

private fun OpenAPI.getOrCreateComponents(): Components {
  if (components == null) {
    components = Components()
  }
  return components
}

