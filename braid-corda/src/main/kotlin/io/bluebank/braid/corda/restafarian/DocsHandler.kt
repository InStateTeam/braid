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
package io.bluebank.braid.corda.restafarian


import io.bluebank.braid.corda.restafarian.Paths.PATH_PARAMS_RE
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON
import io.netty.handler.codec.http.HttpResponseStatus
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.converter.ModelConverters
import io.swagger.models.*
import io.swagger.models.auth.SecuritySchemeDefinition
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.parameters.Parameter
import io.swagger.models.parameters.PathParameter
import io.swagger.models.parameters.QueryParameter
import io.swagger.models.properties.Property
import io.swagger.models.properties.PropertyBuilder
import io.swagger.models.properties.StringProperty
import io.swagger.util.Json
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpMethod.*
import io.vertx.ext.web.RoutingContext
import java.lang.reflect.Type
import java.net.URL
import javax.ws.rs.DefaultValue
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

data class EndPoint(val groupName: String,
                    val method: HttpMethod,
                    val path: String,
                    val name: String,
                    val parameters: List<KParameter>,
                    val returnType: KType,
                    val annotations: List<Annotation>) {

  private val _pathParams = PATH_PARAMS_RE.findAll(path)
      .map { it.groups[2]!!.value }
      .map { paramName -> parameters.firstOrNull { it.name == paramName } }
      .filter { it != null }
      .map { it!! }
      .toList()

  private val _queryParams = parameters - _pathParams

  init {
    // TODO: check sanity of method paramters and types vs REST/HTTP limitations
  }

  fun pathParams(): List<KParameter> {
    return _pathParams
  }

  fun queryParams(): List<KParameter> {
    return _queryParams
  }

  fun bodyParameter(): KParameter? {
    val remaining = parameters.subtract(_pathParams)
    return remaining.lastOrNull()
  }

  val description : String
    get() {
      return annotations.filter { it is ApiOperation }.map { it as ApiOperation}.map { it.value }.firstOrNull() ?: ""
    }
}

class DocsHandler(
    private val serviceName: String = "",
    private val description: String = "",
    private val basePath: String = "http://localhost:8080",
    private val scheme: Scheme = Scheme.HTTPS,
    private val contact: Contact = Contact()
        .name("")
        .email("")
        .url(""),
    private val auth: SecuritySchemeDefinition?
) : Handler<RoutingContext> {
  private var currentGroupName: String = ""
  private val endpoints = mutableListOf<EndPoint>()
  private val models = mutableMapOf<String, Model>()

  override fun handle(context: RoutingContext) {
    val swagger = createSwagger()
    swagger.addAllModels(models)
    endpoints.forEach {
      swagger.addEndpoint(it)
    }
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
            securityDefinition("Authorization", auth)
          }
        }
        .scheme(scheme)
        .consumes(APPLICATION_JSON.toString())
        .produces(APPLICATION_JSON.toString())
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

    val operation = Operation().consumes(APPLICATION_JSON.toString())
    operation.description = endpoint.description

    decorateOperationWithResponseType(endpoint, operation)
    operation.parameters = endpoint.toSwaggerParams()
    operation.tag(endpoint.groupName)
    val swaggerPath = endpoint.path.toSwaggerPath()

    val path = if (this.paths != null && this.paths.contains(swaggerPath)) {
      paths[swaggerPath]!!
    } else {
      val path = Path()
      this.path(swaggerPath, path)
      path
    }

    when (endpoint.method) {
      GET -> path.get(operation)
      POST -> path.post(operation)
      PUT -> path.put(operation)
      DELETE -> path.delete(operation)
      PATCH -> path.patch(operation)
      OPTIONS, HEAD, TRACE, CONNECT, OTHER -> TODO("Implement ${endpoint.method.name}")
    }
  }

  private fun decorateOperationWithResponseType(endpoint: EndPoint, operation: Operation) {
    when (endpoint.returnType.getKType().classifier) {
      Unit::class, Void::class -> {
        // we don't decorate the swagger definition with void types
      }
      else -> {
        val responseSchema = endpoint.returnType.getSwaggerProperty()
        operation
            .produces(responseSchema.toMediaType())
            .defaultResponse(Response().schema(responseSchema))
      }
    }
  }

  private fun Property.toMediaType(): String {
    return when (this) {
      is StringProperty -> HttpHeaderValues.TEXT_PLAIN.toString()
      else -> HttpHeaderValues.APPLICATION_JSON.toString()
    }
  }

  fun <Response> add(groupName: String, method: HttpMethod, path: String, handler: KCallable<Response>) {
    val endpoint = EndPoint(groupName, method, path, handler.name, handler.parameters, handler.returnType, handler.annotations)
    add(endpoint)
  }

  private fun EndPoint.toSwaggerParams(): List<Parameter> {
    if (this.parameters.isEmpty()) return emptyList()

    return when (method) {
      GET -> {
        val pathParams = mapPathParameters()
        val queryParams = mapQueryParameters()
        return pathParams + queryParams
      }
      else -> {
        val pathParameters = mapPathParameters()
        val bodyParameter = mapBodyParameter()
        if (bodyParameter != null) {
          pathParameters + bodyParameter
        } else {
          pathParameters
        }
      }
    }
  }

  private fun EndPoint.mapBodyParameter(): Parameter? {
    val bodyParameter = this.bodyParameter()
    return if (bodyParameter != null) {
      val p = BodyParameter()
          .schema(bodyParameter.type.getSwaggerModelReference())
          .setExamples(bodyParameter)
      p.name = bodyParameter.name
      p.required = true
      p
    } else {
      null
    }
  }

  private fun BodyParameter.setExamples(parameter: KParameter): BodyParameter {
    val example = parameter.findAnnotation<ApiParam>()?.example ?: return this
    this.example(APPLICATION_JSON.toString(), example)
    return this
  }

  private fun EndPoint.mapQueryParameters(): List<Parameter> {
    return queryParams().map { param ->
      val q = QueryParameter()
          .name(param.name)
          .property(param.type.getSwaggerProperty())
      param.findAnnotation<DefaultValue>()?.apply {
        q.setDefaultValue(this.value)
      }
      q.required = true
      if (param.isOptional) {
        q.minItems = 0
      }
      if (param.isVararg) {
        q.minItems = 0
      }
      q
    }
  }

  private fun EndPoint.mapPathParameters(): List<Parameter> {
    return pathParams().map { pathParam ->
      val swaggerProperty = pathParam.type.getSwaggerProperty()
      val p = PathParameter()
          .name(pathParam.name)
          .property(swaggerProperty)
          .type(swaggerProperty.type)
      pathParam.findAnnotation<DefaultValue>()?.apply {
        p.setDefaultValue(this.value)
      }
      p.required = true
      p
    }
  }

  private fun add(endpoint: EndPoint) {
    endpoints.add(endpoint)
    addType(endpoint.returnType)
    endpoint.parameters.forEach {
      addType(it.type)
    }
  }

  private fun addType(type: KType) {
    if (type.jvmErasure == Future::class) {
      addType(type.arguments[0].type!!)
    } else {
      models += type.createSwaggerModels()
      type.arguments.forEach {
        addType(it.type!!)
      }
    }
  }

  private fun Swagger.addAllModels(types: Map<String, Model>): Swagger {
    types.forEach { name, model ->
      this.model(name, model)
    }
    return this
  }

  private fun KType.createSwaggerModels(): Map<String, Model> {
    return ModelConverters.getInstance().readAll(this.javaType)
  }

  private fun KType.getSwaggerModelReference(): Model {
    val property = getSwaggerProperty()
    return PropertyBuilder.toModel(property)
  }

  private fun KType.getSwaggerProperty(): Property {
    return getKType().javaType.getSwaggerProperty()
  }

  private fun Type.getSwaggerProperty(): Property {
    return ModelConverters.getInstance().readAsProperty(this)
  }

  private fun KType.getKType(): KType {
    return if (jvmErasure.java == Future::class.java) {
      this.arguments.last().type!!
    } else {
      this
    }
  }

  fun group(groupId: String, fn: () -> Unit) {
    this.currentGroupName = groupId
    fn()
  }
}

