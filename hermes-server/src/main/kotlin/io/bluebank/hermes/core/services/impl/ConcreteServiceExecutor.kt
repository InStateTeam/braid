package io.bluebank.hermes.core.services.impl

import io.bluebank.hermes.core.jsonrpc.JsonRPCErrorResponse
import io.bluebank.hermes.core.jsonrpc.JsonRPCMounter
import io.bluebank.hermes.core.jsonrpc.JsonRPCRequest
import io.bluebank.hermes.core.jsonrpc.createJsonException
import io.bluebank.hermes.core.jsonschema.describeClass
import io.bluebank.hermes.core.services.ServiceExecutor
import io.bluebank.hermes.server.MethodDescription
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
        .filter { !it.name.contains("$")}
        .map { it.toDescriptor() }
  }

  private fun Method.toDescriptor(): MethodDescriptor {
    val serviceAnnotation = getAnnotation<MethodDescription>(MethodDescription::class.java)
    val name = this.name
    val params = parameters.map { it.type.toJavascriptType() }

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

  private fun Class<*>.toJavascriptType(): String {
    return describeClass(this)
  }

  data class MethodDescriptor(val name: String, val description: String, val parameters: List<String>, val returnType: String)
}