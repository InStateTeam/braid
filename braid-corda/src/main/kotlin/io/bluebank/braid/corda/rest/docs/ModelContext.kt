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

import io.bluebank.braid.corda.swagger.CustomModelConverterV2
import io.bluebank.braid.core.logging.loggerFor
import io.netty.buffer.ByteBuf
import io.swagger.converter.ModelConverters
import io.swagger.models.Model
import io.swagger.models.Swagger
import io.swagger.models.properties.BinaryProperty
import io.swagger.models.properties.Property
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.ByteBuffer

class ModelContext {
  private val mutableModels = mutableMapOf<String, Model>()
  private val modelConverters = ModelConverters().apply { addConverter(CustomModelConverterV2()) }
  val models: Map<String, Model> get() = mutableModels

  fun getProperty(type: Type): Property {
    addType(type)
    val actualType = type.actualType()
    return try {
      if (actualType.isBinary()) {
        BinaryProperty()
      } else {
        modelConverters.readAsProperty(actualType)
      }
    } catch (e: Throwable) {
      throw RuntimeException("Unable to convert actual type: $actualType", e)
    }
  }

  private fun addType(type: Type) {
    if (type is ParameterizedType) {
      if (Future::class.java.isAssignableFrom(type.rawType as Class<*>)) {
        this.addType(type.actualTypeArguments[0])
      } else {
        type.actualTypeArguments.forEach {
          addType(it)
        }
      }
    } else if (!type.isBinary() && type != Unit::class.java && type != Void::class.java) {
      type.createSwaggerModels()
    }
  }


  fun addToSwagger(swagger: Swagger): Swagger {
    models.forEach { (name, model) ->
      try {
        swagger.model(name, model)
      } catch (e: Throwable) {
        log.error("Unable to model class:$name", e)
        throw RuntimeException("Unable to model class:$name", e)
      }
    }
    return swagger
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

  private fun Type.actualType(): Type {
    return if (this is ParameterizedType && Future::class.java.isAssignableFrom(this.rawType as Class<*>)) {
      this.actualTypeArguments[0]
    } else {
      this
    }
  }

  private fun Type.createSwaggerModels() {
    val actualType = actualType()
    val models = modelConverters.readAll(actualType)
    this@ModelContext.mutableModels += models
  }

  companion object {
    private val log = loggerFor<ModelContext>()
  }
}