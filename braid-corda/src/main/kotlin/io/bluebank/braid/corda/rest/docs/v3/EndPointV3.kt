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

import io.bluebank.braid.corda.rest.docs.javaTypeIncludingSynthetics
import io.bluebank.braid.corda.rest.nonEmptyOrNull
import io.bluebank.braid.core.annotation.MethodDescription
import io.netty.buffer.ByteBuf
import io.swagger.v3.core.converter.ResolvedSchema
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.ByteBuffer
import javax.ws.rs.core.MediaType
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

abstract class EndPointV3(
  private val groupName: String,
  val protected: Boolean,
  val method: HttpMethod,
  val path: String,
  private val modelContext: ModelContextV3
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
      annotations: List<Annotation>,
      modelContext: ModelContextV3
    ): EndPointV3 {
      return KEndPointV3(
        groupName,
        protected,
        method,
        path,
        name,
        parameters,
        returnType.javaTypeIncludingSynthetics(),
        annotations,
        modelContext
      ).resolveTypes()
    }

    fun create(
      groupName: String,
      protected: Boolean,
      method: HttpMethod,
      path: String,
      fn: RoutingContext.() -> Unit,
      modelContext: ModelContextV3
    ): EndPointV3 {
      return ImplicitParamsEndPointV3(groupName, protected, method, path, fn, modelContext).resolveTypes()
    }
  }

  abstract val returnType: Type
  abstract val parameterTypes: List<Type>
  protected abstract val annotations: List<Annotation>

  private val apiOperation: io.swagger.v3.oas.annotations.Operation? by lazy {
    annotations.filterIsInstance<io.swagger.v3.oas.annotations.Operation>().firstOrNull()
  }

  private val methodDescription: MethodDescription? by lazy {
    annotations.filterIsInstance<MethodDescription>().firstOrNull()
  }

  val description: String
    get() {
      return methodDescription?.description?.nonEmptyOrNull()
        ?: apiOperation?.description?.nonEmptyOrNull()
        ?: ""
    }

  internal fun resolveTypes(): EndPointV3 {
    modelContext.addType(this.returnType)
    this.parameterTypes.forEach {
      modelContext.addType(it)
    }
    return this
  }

  fun toOperation(): Operation {
    val operation = Operation()
      // todo .consumes(consumes)
      .description(description)
      .parameters(toSwaggerParams())
      .requestBody(mapBodyParameter())
      .addTagsItem(groupName)

    if (protected) {
      operation.addSecurityItem(SecurityRequirement().addList(DocsHandlerV3.SECURITY_DEFINITION_NAME, listOf()))
    }

    operation.responses(
      ApiResponses()
        .addApiResponse("200", response())
        .addApiResponse("500", ApiResponse().description("server failure"))
    )

    return operation
  }

  protected abstract fun mapBodyParameter(): RequestBody?
  protected abstract fun mapQueryParameters(): List<Parameter>
  protected abstract fun mapPathParameters(): List<Parameter>

  protected open fun toSwaggerParams(): List<Parameter> {
    val pathParams = mapPathParameters()
    val queryParams = mapQueryParameters()
    return pathParams + queryParams
  }

  protected fun KType.getSwaggerProperty(): ResolvedSchema {
    return getKType().javaTypeIncludingSynthetics().getSwaggerProperty()
  }

  protected fun KType.getSchema(): Schema<*> {
    return getSwaggerProperty().schema
  }

  protected fun Type.getSwaggerProperty(): ResolvedSchema = modelContext.addType(this)

  private fun response(): ApiResponse {
    val actualReturnType = returnType.actualType()
    if (actualReturnType == Unit::class.java ||
      actualReturnType == Void::class.java ||
      actualReturnType.typeName == "void"
    ) {
      return ApiResponse().description("empty response")
    } else {
      val responseSchema = returnType.getSwaggerProperty()
      return ApiResponse()
        .description("")
        .content(
          Content()
            .addMediaType(
              MediaType.APPLICATION_JSON,
              io.swagger.v3.oas.models.media.MediaType().schema(responseSchema.schema)
            )
        )
    }
  }

  private fun KType.getKType(): KType {
    return if (jvmErasure.java == Future::class.java) {
      this.arguments.last().type!!
    } else {
      this
    }
  }

  /**
   * @return true iff Type is for binary data
   */
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
      actualTypeArguments[0]
    } else {
      this
    }
  }
}
