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

import io.netty.handler.codec.http.HttpHeaderValues
import io.swagger.annotations.ApiOperation
import io.swagger.converter.ModelConverters
import io.swagger.models.Model
import io.swagger.models.Operation
import io.swagger.models.Response
import io.swagger.models.parameters.Parameter
import io.swagger.models.properties.Property
import io.swagger.models.properties.PropertyBuilder
import io.swagger.models.properties.StringProperty
import io.vertx.core.Future
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

abstract class EndPoint {
  companion object {
    fun create(groupName: String, method: HttpMethod, path: String, name: String, parameters: List<KParameter>, returnType: KType, annotations: List<Annotation>): EndPoint{
      return KEndPoint(groupName, method, path, name, parameters, returnType.javaType, annotations)
    }

    fun create(groupName: String, method: HttpMethod, path: String, fn: RoutingContext.() -> Unit) : EndPoint {
      return ImplicitParamsEndPoint(groupName, method, path, fn)
    }
  }
  abstract val groupName: String
  abstract val method: HttpMethod
  abstract val path: String
  abstract val returnType: Type
  protected abstract val annotations: List<Annotation>
  abstract val parameterTypes: List<Type>

  val description: String
    get() {
      return annotations.filter { it is ApiOperation }.map { it as ApiOperation }.map { it.value }.firstOrNull() ?: ""
    }

  fun addTypes(models: MutableMap<String, Model>) {
    addType(this.returnType, models)
    this.parameterTypes.forEach {
      addType(it, models)
    }
  }

  fun toOperation(): Operation {
    val operation = Operation().consumes(HttpHeaderValues.APPLICATION_JSON.toString())
    operation.description = description
    decorateOperationWithResponseType(operation)
    operation.parameters = toSwaggerParams()
    operation.tag(groupName)
    return operation
  }

  protected abstract fun mapBodyParameter(): Parameter?
  protected abstract fun mapQueryParameters(): List<Parameter>
  protected abstract fun mapPathParameters(): List<Parameter>

  protected open fun toSwaggerParams(): List<Parameter> {
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

  protected fun KType.getSwaggerProperty(): Property {
    return getKType().javaType.getSwaggerProperty()
  }


  protected fun KType.getSwaggerModelReference(): Model {
    val property = getSwaggerProperty()
    return PropertyBuilder.toModel(property)
  }

  private fun decorateOperationWithResponseType(operation: Operation) {
    when (returnType) {
      Unit::class.java, Void::class.java -> {
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

  private fun addType(type: Type, models: MutableMap<String, Model>) {
    if (type is ParameterizedType) {
      if (type.rawType == Future::class) {
        this.addType(type.actualTypeArguments[0], models)
      } else {
        type.actualTypeArguments.forEach {
          addType(it, models)
        }
      }
    } else {
      models += type.createSwaggerModels()
    }
  }

  private fun Type.createSwaggerModels(): Map<String, Model> {
    return ModelConverters.getInstance().readAll(this)
  }

  private fun KType.getKType(): KType {
    return if (jvmErasure.java == Future::class.java) {
      this.arguments.last().type!!
    } else {
      this
    }
  }

  private fun Type.getSwaggerProperty(): Property {
    return ModelConverters.getInstance().readAsProperty(this)
  }

  private fun Property.toMediaType(): String {
    return when (this) {
      is StringProperty -> HttpHeaderValues.TEXT_PLAIN.toString()
      else -> HttpHeaderValues.APPLICATION_JSON.toString()
    }
  }

}