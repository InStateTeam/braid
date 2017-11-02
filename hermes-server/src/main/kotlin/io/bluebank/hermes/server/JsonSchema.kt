package io.bluebank.hermes.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.jsonSchema.JsonSchema
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj

fun describeClass(clazz: Class<*>) : String {
  val mapper = ObjectMapper()
  val visitor = SchemaFactoryWrapper()
  mapper.acceptJsonFormatVisitor(clazz, visitor)
  return describe(visitor.finalSchema()).replace("\"", "")
}

private fun describe(value: JsonSchema) : String {
  return describeAsObject(value).toString()
}


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