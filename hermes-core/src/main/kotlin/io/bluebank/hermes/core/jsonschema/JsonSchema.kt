package io.bluebank.hermes.core.jsonschema

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.jsonSchema.JsonSchema
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema
import io.bluebank.hermes.core.annotation.MethodDescription
import io.bluebank.hermes.core.service.MethodDescriptor
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import rx.Observable
import java.lang.reflect.Constructor
import java.lang.reflect.Method

fun Method.toDescriptor(): MethodDescriptor {
  val serviceAnnotation = getAnnotation<MethodDescription>(MethodDescription::class.java)
  val name = this.name
  val params = parameters.map { it.name to it.type.toJavascriptType() }.toMap()

  val returnDescription = if (serviceAnnotation != null && serviceAnnotation.returnType != Any::class) {
    serviceAnnotation.returnType.javaObjectType.toJavascriptType()
  } else {
    returnType.toJavascriptType()
  }
  val returnPrefix = if (returnType == Observable::class.java) {
    "stream-of "
  } else {
    ""
  }
  val description = serviceAnnotation?.description ?: ""
  return MethodDescriptor(name, description, params, returnPrefix + returnDescription)
}

fun <T: Any> Constructor<T>.toDescriptor() : MethodDescriptor {
  val serviceAnnotation = getAnnotation<MethodDescription>(io.bluebank.hermes.core.annotation.MethodDescription::class.java)
  val name = this.name
  val params = parameters.map { it.name to it.type.toJavascriptType() }.toMap()

  val returnDescription = if (serviceAnnotation != null && serviceAnnotation.returnType != kotlin.Any::class) {
    serviceAnnotation.returnType.javaObjectType.toJavascriptType()
  } else {
    this.declaringClass.toJavascriptType()
  }
  val description = serviceAnnotation?.description ?: ""
  return io.bluebank.hermes.core.service.MethodDescriptor(name, description, params, returnDescription)
}

fun Class<*>.toJavascriptType(): String = describeClass(this)

fun Class<*>.toSimpleJavascriptType() : String = describeClassSimple(this)

fun describeClassSimple(clazz: Class<*>) : String {
  return if (clazz.isPrimitive || clazz == String::class.java) {
    describeClass(clazz)
  } else if (clazz.isArray) {
    "array"
  } else {
    clazz.simpleName
  }
}

fun describeClass(clazz: Class<*>) : String {
  val mapper = ObjectMapper()
  val visitor = SchemaFactoryWrapper()
  mapper.acceptJsonFormatVisitor(clazz, visitor)
  return describe(visitor.finalSchema()).replace("\"", "")
}

private fun describe(value: JsonSchema) : String = describeAsObject(value).toString()


private fun describeAsObject(value: JsonSchema): Any {
  return if (value is ObjectSchema) {
    describeProperties(value)
  } else {
    value.type.value()
  }
}

private fun describeProperties(value: ObjectSchema) : JsonObject {
  return json { obj {
    val jo = this
    value.properties.forEach {
      jo.put(it.key, describeAsObject(it.value))
    }
  }}
}