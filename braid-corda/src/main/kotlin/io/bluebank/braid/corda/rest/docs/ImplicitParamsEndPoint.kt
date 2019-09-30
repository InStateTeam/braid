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

import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import io.swagger.models.parameters.*
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import java.lang.reflect.Type
import kotlin.reflect.KAnnotatedElement

class ImplicitParamsEndPoint(
  groupName: String,
  protected: Boolean,
  method: HttpMethod,
  path: String,
  modelContext: ModelContext,
  fn: (RoutingContext) -> Unit
) : EndPoint(groupName, protected, method, path, modelContext) {

  private val annotated = (fn as KAnnotatedElement)
  override val annotations: List<Annotation> = annotated.annotations
  private val implicitParams: List<ApiImplicitParam> =
    annotations.filter { it is ApiImplicitParams || it is ApiImplicitParam }
      .flatMap {
        when (it) {
          is ApiImplicitParams -> it.value.toList()
          is ApiImplicitParam -> listOf(it)
          else -> throw IllegalArgumentException()
        }
      }

  override val parameterTypes: List<Type>
    get() = implicitParams.map {
      it.dataTypeClass.java
    }

  override val returnType: Type
    get() = annotations.filter { it is ApiOperation }.map { it as ApiOperation }.map {
      it.response.java
    }.firstOrNull()
      ?: Unit::class.java

  private val pathParams = implicitParams.filter { it.paramType == "path" }
  private val queryParams = implicitParams.filter { it.paramType == "query" }
  private val bodyParam = implicitParams.singleOrNull { it.paramType == "body" }

  override fun mapBodyParameter(): BodyParameter? {
    return bodyParam?.getSwaggerParameter() as BodyParameter?
  }

  override fun mapQueryParameters(): List<QueryParameter> {
    return queryParams.map { queryParam ->
      queryParam.getSwaggerParameter() as QueryParameter
    }
  }

  override fun mapPathParameters(): List<PathParameter> {
    return pathParams.map { pathParam ->
      pathParam.getSwaggerParameter() as PathParameter
    }
  }

  private fun BodyParameter.setExamples(parameter: ApiImplicitParam): BodyParameter {
    this.examples = parameter.examples.value.map { it.mediaType to it.value }.toMap()
    return this
  }

  private fun ApiImplicitParam.getSwaggerParameter(): Parameter {
    val ip = this
    return when (paramType) {
      "query" -> {
        QueryParameter().apply {
          setProperty(ip.getDataType().getSwaggerProperty())
          setDefaultValue(ip.defaultValue)
          setExample(ip.firstExample())
          type(ip.type)
        }
      }
      "path" -> {
        PathParameter().apply {
          setProperty(ip.getDataType().getSwaggerProperty())
          setDefaultValue(ip.defaultValue)
          setExample(ip.firstExample())
          type(ip.type)
        }
      }
      "body" -> {
        BodyParameter().apply {
          schema = ip.getDataType().getSwaggerModelReference()
          setExamples(ip)
        }
      }
      "header" -> {
        HeaderParameter().apply {
          setProperty(ip.getDataType().getSwaggerProperty())
          setDefaultValue(ip.defaultValue)
          setExample(ip.firstExample())
          type(ip.type)
        }
      }
      "form" -> {
        @Suppress("USELESS_CAST")
        FormParameter().apply {
          setProperty(ip.getDataType().getSwaggerProperty())
          setDefaultValue(ip.defaultValue)
          setExample(ip.firstExample())
          type(ip.type)
        } as Parameter // required to resolve the when statement to the correct type - Kotlin compiler bug?
      }
      else -> {
        throw IllegalArgumentException("unknown parameter type $paramType")
      }
    }.apply {
      name = ip.name
      required = ip.required
    }
  }

  private fun ApiImplicitParam.getDataType(): Type {
    return when {
      this.dataType != "" -> Class.forName(this.dataType)
      else -> this.dataTypeClass.java
    }
  }

  private fun ApiImplicitParam.firstExample(): String {
    return when {
      example != "" -> example
      examples.value.isNotEmpty() && examples.value.first().value != "" -> examples.value.first().value
      else -> ""
    }
  }
}
