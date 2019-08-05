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

import io.bluebank.braid.corda.rest.nonEmptyOrNull
import io.bluebank.braid.core.annotation.MethodDescription
import io.netty.buffer.ByteBuf
import io.swagger.annotations.ApiOperation
import io.swagger.converter.ModelConverters
import io.swagger.models.Model
import io.swagger.models.Operation
import io.swagger.models.Response
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.parameters.Parameter
import io.swagger.models.parameters.PathParameter
import io.swagger.models.parameters.QueryParameter
import io.swagger.models.properties.ArrayProperty
import io.swagger.models.properties.BinaryProperty
import io.swagger.models.properties.Property
import io.swagger.models.properties.PropertyBuilder
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpMethod.*
import io.vertx.ext.web.RoutingContext
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.ByteBuffer
import javax.ws.rs.core.MediaType
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure

abstract class EndPoint(
  private val groupName: String,
  val protected: Boolean,
  val method: HttpMethod,
  val path: String
) {

  companion object {
    fun create(
      groupName: String,
      protected: Boolean,
      method: HttpMethod,
      path: String,
      name: String,
      parameters: List<KParameter>,
      returnType: KType,
      annotations: List<Annotation>
    ): EndPoint {
      return KEndPoint(
        groupName,
        protected,
        method,
        path,
        name,
        parameters,
        returnType.javaType,
        annotations
      )
    }

    fun create(
      groupName: String,
      protected: Boolean,
      method: HttpMethod,
      path: String,
      fn: RoutingContext.() -> Unit
    ): EndPoint {
      return ImplicitParamsEndPoint(groupName, protected, method, path, fn)
    }
  }

  abstract val returnType: Type
  protected abstract val annotations: List<Annotation>
  abstract val parameterTypes: List<Type>

  private val apiOperation: ApiOperation? by lazy {
    annotations.filterIsInstance<ApiOperation>().firstOrNull()
  }

  private val methodDescription: MethodDescription? by lazy {
    annotations.filterIsInstance<MethodDescription>().firstOrNull()
  }

  val responseContainer: String?
    get() {
      return apiOperation?.responseContainer
    }

  val description: String
    get() {
      return methodDescription?.description?.nonEmptyOrNull()
        ?: apiOperation?.value?.nonEmptyOrNull()
        ?: ""
    }

  open val produces: String
    get() {
      return if (apiOperation != null && !apiOperation!!.produces.isBlank()) {
        apiOperation!!.produces
      } else {
        returnType.mediaType()
      }
    }

  open val consumes: String
    get() {
      return if (apiOperation != null && !apiOperation!!.consumes.isBlank()) {
        apiOperation!!.consumes
      } else {
        mapBodyParameter()?.schema?.properties?.keys?.first() ?: returnType.mediaType()
      }
    }

  fun addTypes(models: MutableMap<String, Model>) {
    addType(this.returnType, models)
    this.parameterTypes.forEach {
      addType(it, models)
    }
  }

  fun toOperation(): Operation {
    val operation = Operation().consumes(consumes)
    operation.description = description
    decorateOperationWithResponseType(operation)
    operation.parameters = toSwaggerParams()
    operation.tag(groupName)
    if (protected) {
      operation.addSecurity(DocsHandler.SECURITY_DEFINITION_NAME, listOf())
    }
    operation.addResponse("200", operation.responses["default"])
    operation.addResponse("500", Response().description("server failure"))
    return operation
  }

  protected abstract fun mapBodyParameter(): BodyParameter?
  protected abstract fun mapQueryParameters(): List<QueryParameter>
  protected abstract fun mapPathParameters(): List<PathParameter>

  protected open fun toSwaggerParams(): List<Parameter> {
    return when (method) {
      GET, HEAD, DELETE, CONNECT, OPTIONS  -> {
        val pathParams = mapPathParameters()
        val queryParams = mapQueryParameters()
        return pathParams + queryParams
      }
      else -> {
        val pathParameters = mapPathParameters() as List<Parameter>
        val queryParams = mapQueryParameters()
        val bodyParameter = mapBodyParameter()
        if (bodyParameter != null) {
          pathParameters + queryParams + bodyParameter
        } else {
          pathParameters + queryParams
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

  protected fun Type.getSwaggerModelReference(): Model {
    val property = getSwaggerProperty()
    return PropertyBuilder.toModel(property)
  }

  protected fun Type.getSwaggerProperty(): Property {
    val actualType = this.actualType()
    return if (actualType.isBinary()) {
      BinaryProperty()
    } else {
      ModelConverters.getInstance().readAsProperty(actualType)
    }
  }

  private fun decorateOperationWithResponseType(operation: Operation) {
    val actualReturnType = returnType.actualType()
    if (actualReturnType == Unit::class.java ||
      actualReturnType == Void::class.java ||
      actualReturnType.typeName == "void"
    ) {
      operation
        .produces(MediaType.TEXT_PLAIN)
        .defaultResponse(Response().description("empty response"))
    } else {
      val responseSchema = returnType.getSwaggerProperty().let { responseSchema ->
        when (responseContainer) {
          "List", "Array", "Set" -> {
            ArrayProperty(responseSchema)
          }
          else -> {
            responseSchema
          }
        }

      }
      @Suppress("DEPRECATION")
      operation
        .produces(produces)
        .defaultResponse(Response().schema(responseSchema).description("default response"))
    }
  }

  private fun addType(type: Type, models: MutableMap<String, Model>) {
    if (type is ParameterizedType) {
      if (Future::class.java.isAssignableFrom(type.rawType as Class<*>)) {
        this.addType(type.actualTypeArguments[0], models)
      } else {
        type.actualTypeArguments.forEach {
          addType(it, models)
        }
      }
    } else if (!type.isBinary() && type != Unit::class.java && type != Void::class.java) {
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

  private fun Type.isBinary(): Boolean {
    return when (this) {
      Buffer::class.java,
      ByteArray::class.java,
      ByteBuffer::class.java,
      ByteBuf::class.java -> true
      else -> false
    }
  }

  protected fun Type.mediaType(): String {
    val actualType = this.actualType()
    return when {
      actualType.isBinary() -> MediaType.APPLICATION_OCTET_STREAM
      actualType == String::class.java -> MediaType.TEXT_PLAIN
      else -> MediaType.APPLICATION_JSON
    }
  }

  private fun Type.actualType(): Type {
    return if (this is ParameterizedType && Future::class.java.isAssignableFrom(this.rawType as Class<*>)) {
      this.actualTypeArguments[0]
    } else {
      this
    }
  }
}