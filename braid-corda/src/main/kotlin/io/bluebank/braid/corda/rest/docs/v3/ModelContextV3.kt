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

import com.fasterxml.jackson.annotation.JsonTypeInfo
import io.bluebank.braid.corda.swagger.v3.CustomModelConverterV3
import io.bluebank.braid.corda.swagger.v3.JSR310ModelConverterV3
import io.netty.buffer.ByteBuf
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.core.converter.ResolvedSchema
import io.swagger.v3.core.util.Json
import io.swagger.v3.core.util.PrimitiveType
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.BinarySchema
import io.swagger.v3.oas.models.media.Schema
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import net.corda.core.utilities.contextLogger
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.ByteBuffer

class ModelContextV3 {
  companion object {
    private val log = contextLogger()
    private val binaryResolvedSchema = ResolvedSchema().apply { schema = BinarySchema() }
  }

  private val mutableModels = mutableMapOf<String, Schema<*>>()
  val models: Map<String, Schema<*>> get() = mutableModels
  private val modelConverters = ModelConverters().apply {
    addConverter(QualifiedTypeNameConverter(io.vertx.core.json.Json.mapper))
    addConverter(JSR310ModelConverterV3())
    addConverter(CustomModelConverterV3())
  }

  fun addType(type: Type): ResolvedSchema {
    // todo move to CustomModelConverter
    val actualType = type.actualType()
    return try {
      when {
        actualType.isBinary() -> binaryResolvedSchema
        else -> {
          actualType.createSwaggerModels()?.also {
            mutableModels += it.referencedSchemas
          } ?: ResolvedSchema().apply {
            this.schema = PrimitiveType.createProperty(actualType)
          }
        }
      }
    } catch (e: Throwable) {
      throw RuntimeException("Unable to convert actual type: $actualType", e)
    }
  }

  fun addToSwagger(openApi: OpenAPI): OpenAPI {
    models.forEach { (name, model) ->
      try {
        openApi.schema(name, model)
      } catch (e: Throwable) {
        log.error("Unable to model class:$name", e)
        throw RuntimeException("Unable to model class:$name", e)
      }
    }
    return openApi
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

  private fun Type.createSwaggerModels(): ResolvedSchema? {
    return modelConverters.resolveAsResolvedSchema(AnnotatedType(this)
        .resolveAsRef(true))
  }

  private fun Type.actualType(): Type {
    return if (this is ParameterizedType && Future::class.java.isAssignableFrom(this.rawType as Class<*>)) {
      this.actualTypeArguments[0]
    } else {
      this
    }
  }
}