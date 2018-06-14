/*
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

import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiOperation
import io.swagger.models.parameters.*
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import java.lang.reflect.Type
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KType
import kotlin.reflect.full.createType

class ImplicitParamsEndPoint(
  override val groupName: String,
  override val method: HttpMethod,
  override val path: String,
  fn: (RoutingContext) -> Unit) : EndPoint() {

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

  private val pathParams = implicitParams.filter { it.type == "path" }
  private val queryParams = implicitParams.filter { it.type == "query" }
  private val bodyParam = implicitParams.singleOrNull { it.type == "body" }

  override fun mapBodyParameter(): Parameter? {
    return bodyParam?.getSwaggerParameter()
  }

  override fun mapQueryParameters(): List<Parameter> {
    return queryParams.map { queryParam ->
      queryParam.getSwaggerParameter()
    }
  }

  override fun mapPathParameters(): List<Parameter> {
    return pathParams.map { pathParam ->
      pathParam.getSwaggerParameter()
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
          setExample(ip.example)
          type(ip.type)
        }
      }
      "path" -> {
        PathParameter().apply {
          setProperty(ip.getDataType().getSwaggerProperty())
          setDefaultValue(ip.defaultValue)
          setExample(ip.example)
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
          setExample(ip.example)
          type(ip.type)
        }
      }
      "form" -> {
        @Suppress("USELESS_CAST")
        FormParameter().apply {
          setProperty(ip.getDataType().getSwaggerProperty())
          setDefaultValue(ip.defaultValue)
          setExample(ip.example)
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

  private fun ApiImplicitParam.getDataType(): KType {
    return when {
      this.dataType != "" -> Class.forName(this.dataType).kotlin.createType()
      else -> this.dataTypeClass.createType()
    }
  }
}
