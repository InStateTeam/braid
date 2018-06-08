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

import io.bluebank.braid.corda.restafarian.Paths
import io.netty.handler.codec.http.HttpHeaderValues
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.converter.ModelConverters
import io.swagger.models.Model
import io.swagger.models.Operation
import io.swagger.models.Response
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.parameters.Parameter
import io.swagger.models.parameters.PathParameter
import io.swagger.models.parameters.QueryParameter
import io.swagger.models.properties.Property
import io.swagger.models.properties.PropertyBuilder
import io.swagger.models.properties.StringProperty
import io.vertx.core.Future
import io.vertx.core.http.HttpMethod
import java.lang.reflect.Type
import javax.ws.rs.DefaultValue
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

class KEndPoint(groupName: String,
                method: HttpMethod,
                path: String,
                val name: String,
                val parameters: List<KParameter>,
                private val returnType: KType,
                private val annotations: List<Annotation>) : EndPoint(groupName, method, path) {

  private val _pathParams = Paths.PATH_PARAMS_RE.findAll(path)
    .map { it.groups[2]!!.value }
    .map { paramName -> parameters.firstOrNull { it.name == paramName } }
    .filter { it != null }
    .map { it!! }
    .toList()

  private val _queryParams = parameters - _pathParams

  init {
    // TODO: check sanity of method parameters and types vs REST/HTTP limitations
  }

  private fun pathParams(): List<KParameter> {
    return _pathParams
  }

  private fun queryParams(): List<KParameter> {
    return _queryParams
  }

  private fun bodyParameter(): KParameter? {
    val remaining = parameters.subtract(_pathParams)
    return remaining.lastOrNull()
  }

  override val description: String
    get() {
      return annotations.filter { it is ApiOperation }.map { it as ApiOperation }.map { it.value }.firstOrNull() ?: ""
    }

  override fun toSwaggerParams(): List<Parameter> {
    if (this.parameters.isEmpty()) return emptyList()

    return when (method) {
      HttpMethod.GET -> {
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

  private fun mapPathParameters(): List<Parameter> {
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

  private fun mapQueryParameters(): List<Parameter> {
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

  private fun mapBodyParameter(): Parameter? {
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

  private fun KType.getSwaggerProperty(): Property {
    return getKType().javaType.getSwaggerProperty()
  }

  private fun KType.getKType(): KType {
    return if (jvmErasure.java == Future::class.java) {
      this.arguments.last().type!!
    } else {
      this
    }
  }

  private fun KType.getSwaggerModelReference(): Model {
    val property = getSwaggerProperty()
    return PropertyBuilder.toModel(property)
  }

  private fun Type.getSwaggerProperty(): Property {
    return ModelConverters.getInstance().readAsProperty(this)
  }

  private fun BodyParameter.setExamples(parameter: KParameter): BodyParameter {
    val example = parameter.findAnnotation<ApiParam>()?.example ?: return this
    this.example(HttpHeaderValues.APPLICATION_JSON.toString(), example)
    return this
  }

  override fun decorateOperationWithResponseType(operation: Operation) {
    when (returnType.getKType().classifier) {
      Unit::class, Void::class -> {
        // we don't decorate the swagger definition with void types
      }
      else -> {
        val responseSchema = returnType.getSwaggerProperty()
        operation
          .produces(responseSchema.toMediaType())
          .defaultResponse(Response().schema(responseSchema))
      }
    }
  }

  fun addTypes(models: MutableMap<String, Model>) {
    this.addType(this.returnType, models)
    this.parameters.forEach {
      this.addType(it.type, models)
    }
  }

  private fun addType(type: KType, models: MutableMap<String, Model>) {
    if (type.jvmErasure == Future::class) {
      this.addType(type.arguments[0].type!!, models)
    } else {
      models += type.createSwaggerModels()
      type.arguments.forEach {
        this.addType(it.type!!, models)
      }
    }
  }

  private fun Property.toMediaType(): String {
    return when (this) {
      is StringProperty -> HttpHeaderValues.TEXT_PLAIN.toString()
      else -> HttpHeaderValues.APPLICATION_JSON.toString()
    }
  }

  private fun KType.createSwaggerModels(): Map<String, Model> {
    return ModelConverters.getInstance().readAll(this.javaType)
  }

}