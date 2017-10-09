package io.bluebank.jsonrpc.server

import io.bluebank.jsonrpc.server.JsonRPCErrorPayload.Companion.serverError
import io.bluebank.jsonrpc.server.JsonRPCErrorPayload.Companion.throwMethodNotFound
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Future.failedFuture
import java.lang.reflect.Method
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class MethodDoesNotExist : Exception()

interface ServiceExecutor {
  @Throws(MethodDoesNotExist::class)
  fun invoke(rpcRequest: JsonRPCRequest, resultCallback: (AsyncResult<Any>) -> Unit)
}

class CompositeExecutor(vararg predefinedExecutors: ServiceExecutor) : ServiceExecutor {
  private val executors = mutableListOf(*predefinedExecutors)

  fun add(executor: ServiceExecutor) {
    executors += executor
  }

  override fun invoke(rpcRequest: JsonRPCRequest, resultCallback: (AsyncResult<Any>) -> Unit) {
    if (executors.isEmpty()) {
      resultCallback(serverError(rpcRequest.id, "no services available to call via executor interface").toFailedFuture())
    } else {
      invoke(0, rpcRequest, resultCallback)
    }
  }

  private fun invoke(executorIndex: Int, rpcRequest: JsonRPCRequest, resultCallback: (AsyncResult<Any>) -> Unit) {
    // assert executorIndex will always be within range
    executors[executorIndex].invoke(rpcRequest) { ar ->
      if (ar.succeeded()) {
        resultCallback(ar)
      } else {
        if (executorIndex == executors.size - 1) {
          resultCallback(ar)
        } else {
          invoke(executorIndex + 1, rpcRequest, resultCallback)
        }
      }
    }
  }
}


class ConcreteServiceExecutor(private val service: Any) : ServiceExecutor {
  override fun invoke(request: JsonRPCRequest, callback: (AsyncResult<Any>) -> Unit) {
    val method = findMethod(request)
    val castedParameters = request.mapParams(method)
    try {
      val result = method.invoke(service, *castedParameters)
      handleResult(result, request, callback)
    } catch (err: Throwable) {
      callback(failedFuture(serverError(request.id, err.message, 0)))
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
      else -> respond(serverError(request.id, response.cause().message), callback)
    }
  }

  private fun respond(result: Any?, callback: (AsyncResult<Any>) -> Unit) {
    callback(Future.succeededFuture(result))
  }

  private fun respond(err: Throwable, callback: (AsyncResult<Any>) -> Unit) {
    callback(err.toFailedFuture())
  }

  private fun findMethod(request: JsonRPCRequest): Method {
    try {
      return service.javaClass.methods.single { request.matchesMethod(it) }
    } catch (err: IllegalArgumentException) {
      throwMethodNotFound(request.id, "method ${request.method} has multiple implementations with the same number of parameters")
    } catch (err: NoSuchElementException) {
      throwMethodNotFound(request.id, "could not find method ${request.method}")
    }
  }
}


class JavascriptService : ServiceExecutor {
  override fun invoke(rpcRequest: JsonRPCRequest, resultCallback: (AsyncResult<Any>) -> Unit) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  private val engine: ScriptEngine by lazy {
    val sem = ScriptEngineManager()
    sem.getEngineByName("nashorn")
  }

  private val invocable: Invocable by lazy {
    engine as Invocable
  }
}
