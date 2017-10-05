package io.bluebank.jsonrpc

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.lang.reflect.Method
import java.lang.reflect.Parameter

class JsonSchemaGenerator(val packageSpace: String) {
    fun generate(): List<JsonObject> {
        val list = mutableListOf<JsonObject>()
        FastClasspathScanner(packageSpace).matchClassesWithAnnotation(JRpcService::class.java) { foundClass ->
            val json = buildDescriptor(foundClass)
            list += json
        }.scan()
        return list
    }

    private fun buildDescriptor(foundClass: Class<*>): JsonObject {
        val result = JsonObject()
        val annotation = foundClass.getAnnotation<JRpcService>(JRpcService::class.java)!!
        result.put("description", annotation.description)
        documentMethods(foundClass, result)
        return result
    }

    private fun documentMethods(foundClass: Class<*>, result: JsonObject) {
        foundClass.declaredMethods
            .filter {
                it.isAnnotationPresent(JRpcMethod::class.java)
            }
            .forEach {
                val methodDescription = documentMethod(it)
                result.put(it.name, methodDescription)
            }
    }

    private fun documentMethod(method: Method): JsonObject {
        val result = JsonObject()
        val annotation = method.getAnnotation<JRpcMethod>(JRpcMethod::class.java)
        result
            .put("type", "method")
            .put("description", annotation.description)
            .put("params", documentParameters(method))
            .put("return", documentReturn(method))
        return result
    }

    private fun documentReturn(method: Method): Any {
        val returnType = method.getAnnotation<JRpcMethod>(JRpcMethod::class.java).returnType
        return returnType.java.mapToJsonSchemaType()
    }

    private fun documentParameters(method: Method): JsonArray {
        val result = JsonArray()
        method.parameters.forEach {
            result.add(documentParameter(it))
        }
        return result
    }

    private fun documentParameter(it: Parameter): JsonObject {
        return JsonObject()
    }
}

private fun <T> Class<T>.mapToJsonSchemaType(): Any {
    return when (this) {
        Int::javaClass -> "int"
        Long::javaClass -> "long"
        Double::javaClass -> "number"
        Float::javaClass -> "number"
        String::javaClass -> "string"
        Boolean::javaClass -> "boolean"
        else -> {
            "any"
        }
    }
}


