package io.bluebank.braid.corda.rest.docs

import io.netty.buffer.ByteBuf
import io.swagger.converter.ModelConverters
import io.swagger.models.Model
import io.swagger.models.Swagger
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.ByteBuffer

class ModelContext {
  private val mutableModels = mutableMapOf<String, Model>()
  val models: Map<String, Model> get() = mutableModels

  fun addType(type: Type) {
    if (type is ParameterizedType) {
      if (Future::class.java.isAssignableFrom(type.rawType as Class<*>)) {
        this.addType(type.actualTypeArguments[0])
      } else {
        type.actualTypeArguments.forEach {
          addType(it)
        }
      }
    } else if (!type.isBinary() && type != Unit::class.java && type != Void::class.java) {
      mutableModels += type.createSwaggerModels()
    }
  }

  fun addToSwagger(swagger: Swagger): Swagger {
    models.forEach { (name, model) ->
      try {
        swagger.model(name, model)
      } catch (e: Throwable) {
        DocsHandler.log.error("Unable to model class:$name", e)
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

  private fun Type.createSwaggerModels(): Map<String, Model> {
    return ModelConverters.getInstance().readAll(this)
  }
}