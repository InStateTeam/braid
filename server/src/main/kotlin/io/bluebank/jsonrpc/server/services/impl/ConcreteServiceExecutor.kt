package io.bluebank.jsonrpc.server.services.impl

import io.bluebank.jsonrpc.server.*
import io.bluebank.jsonrpc.server.services.ServiceExecutor
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import rx.Observable
import rx.Subscriber
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class ConcreteServiceExecutor(private val service: Any) : ServiceExecutor {

  override fun invoke(request: JsonRPCRequest): Observable<Any> {
    // use unsafe constructor until we switch to rxjava2 and vertx 3.5.0 - currently too new to be confident of stability
    return Observable.create<Any> { subscriber ->
      try {
        val method = findMethod(request)
        val castedParameters = request.mapParams(method)
        val result = method.invoke(service, *castedParameters)
        handleResult(result, request, subscriber)
      } catch (err: Throwable) {
        subscriber.onError(err)
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun handleResult(result: Any?, request: JsonRPCRequest, subscriber: Subscriber<Any>) {
    when (result) {
      is Future<*> -> handleFuture(result as Future<Any>, request, subscriber)
      is Observable<*> -> handleObservable(result as Observable<Any>, request, subscriber)
      else -> respond(result, subscriber)
    }
  }

  private fun handleObservable(result: Observable<Any>, request: JsonRPCRequest, subscriber: Subscriber<Any>) {
    result
        .onErrorResumeNext { err -> Observable.error(err.createJsonException(request)) }
        .subscribe(subscriber)
  }

  private fun handleFuture(future: Future<Any>, request: JsonRPCRequest, callback: Subscriber<Any>) {
    future.setHandler(JsonRPCMounter.FutureHandler {
      handleAsyncResult(it, request, callback)
    })
  }

  private fun handleAsyncResult(response: AsyncResult<*>, request: JsonRPCRequest, subscriber: Subscriber<Any>) {
    when (response.succeeded()) {
      true -> respond(response.result(), subscriber)
      else -> respond(response.cause().createJsonException(request), subscriber)
    }
  }

  private fun respond(result: Any?, subscriber: Subscriber<Any>) {
    subscriber.onNext(result)
    subscriber.onCompleted()
  }

  private fun respond(err: Throwable, subscriber: Subscriber<Any>) {
    subscriber.onError(err)
  }

  private fun findMethod(request: JsonRPCRequest): Method {
    try {
      return service.javaClass.methods.single { request.matchesMethod(it) }
    } catch (err: IllegalArgumentException) {
      JsonRPCErrorResponse.throwMethodNotFound(request.id, "method ${request.method} has multiple implementations with the same number of parameters")
    } catch (err: NoSuchElementException) {
      JsonRPCErrorResponse.throwMethodNotFound(request.id, "could not find method ${request.method}")
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