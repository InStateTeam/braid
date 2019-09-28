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

import io.bluebank.braid.corda.rest.Paths
import io.bluebank.braid.corda.rest.parameterName
import io.netty.handler.codec.http.HttpHeaderValues
import io.swagger.annotations.ApiParam
import io.swagger.models.parameters.*
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpMethod.*
import java.lang.reflect.Type
import javax.ws.rs.DefaultValue
import javax.ws.rs.QueryParam
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation

class KEndPoint(
  groupName: String,
  protected: Boolean,
  method: HttpMethod,
  path: String,
  val name: String,
  val parameters: List<KParameter>,
  override val returnType: Type,
  override val annotations: List<Annotation>,
  modelContext: ModelContext
) : EndPoint(groupName, protected, method, path, modelContext) {

  init {
    // TODO: check sanity of method parameters and types vs REST/HTTP limitations
  }

  private val contextParameters = parameters.filter { it.findAnnotation<Context>() != null }
  private val pathParamNames = Paths.PATH_PARAMS_RE.findAll(path)
    .map { it.groups[2]!!.value }

  private val pathParams = pathParamNames
    .map { paramName ->
      parameters.firstOrNull { it.parameterName() == paramName }
        ?: error("could not bind path parameter with name $paramName")
    }
    .toList()

  private val bodyParameter: KParameter? =
    when (method) {
      GET, HEAD, DELETE, CONNECT, OPTIONS -> null // can't have body parameters for this - everything that's not a path param, gets bound as query parameter
      else -> parameters.subtract(pathParams).lastOrNull { it.findAnnotation<QueryParam>() == null }
    }

  private val queryParams = parameters.subtract(pathParams).let {
    when (bodyParameter) {
      null -> it - contextParameters
      else -> it - contextParameters - bodyParameter
    }
  }

  override val consumes: String
    get() {
      return bodyParameter?.type?.javaTypeIncludingSynthetics()?.mediaType()
        ?: MediaType.APPLICATION_JSON
    }

  override val parameterTypes: List<Type>
    get() = ((pathParams + queryParams).let {
      when {
        bodyParameter != null -> it + bodyParameter
        else -> it
      }
    }).map { it.type.javaTypeIncludingSynthetics() }

  override fun toSwaggerParams(): List<Parameter> {
    return if (this.parameters.isEmpty()) {
      return emptyList()
    } else {
      super.toSwaggerParams()
    }
  }

  override fun mapPathParameters(): List<PathParameter> {
    return pathParams.map { param ->
      val swaggerProperty = param.type.getSwaggerProperty()
      val p = PathParameter()
        .name(param.parameterName())
        .property(swaggerProperty)
        .type(swaggerProperty.type)
      applyDefaultValueAnnotation(param, p)
      applyApiParamDocs(param, p)
      applyRequiredAndVarArg(param, p)
      p
    }
  }

  override fun mapQueryParameters(): List<QueryParameter> {
    return queryParams.map { param ->
      val q = QueryParameter()
        .name(param.parameterName())
        .property(param.type.getSwaggerProperty())
      applyDefaultValueAnnotation(param, q)
      applyApiParamDocs(param, q)
      applyRequiredAndVarArg(param, q)
      q
    }
  }

  private fun <T : AbstractSerializableParameter<T>> applyDefaultValueAnnotation(
    param: KParameter,
    q: T
  ) {
    param.findAnnotation<DefaultValue>()?.apply {
      if (value.isNotBlank()) q.setDefaultValue(value)
    }
  }

  private fun <T : AbstractSerializableParameter<T>> applyApiParamDocs(
    pathParam: KParameter,
    p: T
  ) {
    pathParam.findAnnotation<ApiParam>()?.apply {
      if (value.isNotBlank()) p.description(value)
      if (name.isNotBlank()) p.name(name)
      if (type.isNotBlank()) p.type(type)
      if (example.isNotBlank()) p.example(example)
      if (defaultValue.isNotBlank()) p.setDefaultValue(defaultValue)
    }
  }

  private fun <T : AbstractSerializableParameter<T>> applyRequiredAndVarArg(
    param: KParameter,
    p: T
  ) {
    p.required = !param.isOptional && !param.isVararg
    p.minItems = if (param.isOptional || param.isVararg) {
      0
    } else {
      p.minItems
    }
  }

  override fun mapBodyParameter(): BodyParameter? {
    return bodyParameter?.let {
      BodyParameter().apply {
        schema(bodyParameter.type.getSwaggerModelReference())
        setExamples(bodyParameter)
        name = bodyParameter.name
        required = true
      }
    }
  }

  private fun BodyParameter.setExamples(parameter: KParameter): BodyParameter {
    val example = parameter.findAnnotation<ApiParam>()?.example ?: return this
    this.example(HttpHeaderValues.APPLICATION_JSON.toString(), example)
    return this
  }

  override fun toString(): String {
    return "KEndPoint(name='$name', parameters=$parameters, returnType=$returnType)"
  }
}