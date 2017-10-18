package io.bluebank.jsonrpc.server.services.impl

import io.bluebank.jsonrpc.server.*
import io.bluebank.jsonrpc.server.services.ServiceExecutor
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class ConcreteServiceExecutor(private val service: Any) : ServiceExecutor {
  override fun invoke(request: JsonRPCRequest, callback: (AsyncResult<Any>) -> Unit) {
    try {
      val method = findMethod(request)
      val castedParameters = request.mapParams(method)
      val result = method.invoke(service, *castedParameters)
      handleResult(result, request, callback)
    } catch (err: Throwable) {
      callback(Future.failedFuture(err))
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun handleResult(result: Any?, request: JsonRPCRequest, callback: (AsyncResult<Any>) -> Unit) {
    when (result) {
      is Future<*> -> handleFuture(result as Future<Any>, request, callback)
      else -> respond(result, callback)
    }
  }

  private fun handleFuture(future: Future<Any>, request: JsonRPCRequest, callback: (AsyncResult<Any>) -> Unit) {
    future.setHandler(JsonRPCMounter.FutureHandler {
      handleAsyncResult(it, request, callback)
    })
  }

  private fun handleAsyncResult(response: AsyncResult<*>, request: JsonRPCRequest, callback: (AsyncResult<Any>) -> Unit) {
    when (response.succeeded()) {
      true -> respond(response.result(), callback)
      else -> respond(JsonRPCErrorPayload.serverError(request.id, response.cause().message), callback)
    }
  }

  private fun respond(result: Any?, callback: (AsyncResult<Any>) -> Unit) {
    callback(Future.succeededFuture(result))
  }

  private fun respond(err: Throwable, callback: (AsyncResult<Any>) -> Unit) {
    callback(Future.failedFuture(err))
  }

  private fun findMethod(request: JsonRPCRequest): Method {
    try {
      return service.javaClass.methods.single { request.matchesMethod(it) }
    } catch (err: IllegalArgumentException) {
      JsonRPCErrorPayload.throwMethodNotFound(request.id, "method ${request.method} has multiple implementations with the same number of parameters")
    } catch (err: NoSuchElementException) {
      JsonRPCErrorPayload.throwMethodNotFound(request.id, "could not find method ${request.method}")
    }
  }

  fun getJavaStubs(): List<MethodDescriptor> {
    return service.javaClass.declaredMethods
      .filter { Modifier.isPublic(it.modifiers) }
      .map { it.toDescriptor() }
  }

  private fun Method.toDescriptor(): MethodDescriptor {
    val serviceAnnotation = getAnnotation<JsonRPCService>(JsonRPCService::class.java)
    val name = serviceAnnotation?.name ?: this.name
    val params = parameters.map { it.type.toJavascriptType() + "Param" }
    val returnType = returnType.toJavascriptType()
    val description = serviceAnnotation?.description ?: ""
    return MethodDescriptor(name, description, params, returnType)
  }


  private fun Class<*>.toJavascriptType(): String {
    return when (this) {
      Int::class.java -> "int"
      Double::class.java -> "double"
      Boolean::class.java -> "bool"
      String::class.java -> "string"
      Array<Any>::class.java -> "array"
      List::class.java -> "array"
      Map::class.java -> "map"
      Collection::class.java -> "array"
      else -> "object"
    }
  }


  data class MethodDescriptor(val name: String, val description: String, val parameters: List<String>, val returnType: String)
}