package io.bluebank.jsonrpc.server

import io.bluebank.jsonrpc.server.JsonRPCErrorPayload.Companion.serverError
import io.bluebank.jsonrpc.server.JsonRPCErrorPayload.Companion.throwMethodNotFound
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Future.*
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import java.lang.reflect.Method
import java.nio.file.Path
import java.nio.file.Paths
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class MethodDoesNotExist : Exception()

interface ServiceExecutor {
  @Throws(MethodDoesNotExist::class)
  fun invoke(request: JsonRPCRequest, callback: (AsyncResult<Any>) -> Unit)
}

class CompositeExecutor(vararg predefinedExecutors: ServiceExecutor) : ServiceExecutor {
  val executors = mutableListOf(*predefinedExecutors)

  fun add(executor: ServiceExecutor) {
    executors += executor
  }

  override fun invoke(request: JsonRPCRequest, callback: (AsyncResult<Any>) -> Unit) {
    if (executors.isEmpty()) {
      callback(failedFuture("no services available to call via executor interface"))
    } else {
      invoke(0, request, callback)
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
      callback(failedFuture(err))
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
    callback(succeededFuture(result))
  }

  private fun respond(err: Throwable, callback: (AsyncResult<Any>) -> Unit) {
    callback(failedFuture(err))
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


class JavascriptExecutor(private val vertx: Vertx, private val name: String) : ServiceExecutor {
  companion object {
    private val logger = loggerFor<JavascriptExecutor>()
    private val SCRIPTS_PATH = ".service-scripts"
    private val sem = ScriptEngineManager()
    private val SCRIPT_ENGINE_NAME = "nashorn"
    fun clearScriptsFolder(vertx: Vertx) : Future<Unit> {
      val future = future<Void>()

      vertx.fileSystem().deleteRecursive(SCRIPTS_PATH, true, future.completer())
      return future
        .map { Unit }
        .otherwise { Unit }
    }
    fun makeScriptsFolder(vertx: Vertx) {
      if (!vertx.fileSystem().existsBlocking(SCRIPTS_PATH)) {
        vertx.fileSystem().mkdirBlocking(SCRIPTS_PATH)
      }
    }
    fun queryServiceNames(vertx: Vertx) : List<String> {
      makeScriptsFolder(vertx)
      return vertx.fileSystem().readDirBlocking(SCRIPTS_PATH)
        .map {
          Paths.get(it).fileName.toString()
        }
        .filter {
          it.endsWith(".js")
        }
        .map {
          it.dropLast(3)
        }
    }
  }

  private val scriptPath = "$SCRIPTS_PATH/$name.js"
  private var engine: ScriptEngine = createEngine()
  private val invocable: Invocable
    get() {
      return engine as Invocable
    }

  init {
    makeScriptsFolder(vertx)
    loadScript()
  }


  fun getScript(): Buffer {
    with(vertx.fileSystem()) {
      return if (existsBlocking(scriptPath)) {
        readFileBlocking(scriptPath)
      } else {
        Buffer.buffer()
      }
    }
  }

  fun updateScript(script: String) {
    engine = createEngine()
    try {
      engine.eval(script)
      saveScript(script)
    } catch (err: Throwable) {
      logger.error("failed to load script", err)
      throw err
    }
  }

  override fun invoke(request: JsonRPCRequest, callback: (AsyncResult<Any>) -> Unit) {
    checkMethodExists(request.method)

    val params = if (request.params != null && request.params is List<*>) {
      request.params.toTypedArray()
    } else {
      listOf(request.params).toTypedArray()
    }

    try {
      val result = invocable.invokeFunction(request.method, *params)
      callback(succeededFuture(result))
    } catch (err: Throwable) {
      callback(failedFuture("failed to invoke rpc"))
    }
  }

  private fun loadScript() {
    try {
      val buffer = getScript()
      engine.eval(buffer.toString())
    } catch (err: Throwable) {
      logger.error("failed to source script $scriptPath", err)
    }
  }

  private fun createEngine(): ScriptEngine {
    return sem.getEngineByName(SCRIPT_ENGINE_NAME)
  }

  private fun saveScript(script: String) {
    with(vertx.fileSystem()) {
      try {
        this.writeFileBlocking(scriptPath, Buffer.buffer(script))
      } catch (err: Throwable) {
        logger.error("failed to write script to $scriptPath", err)
      }
    }
  }

  @Throws(MethodDoesNotExist::class)
  private fun checkMethodExists(methodName: String) {
    val exists = engine.eval("(typeof $methodName) === 'function'") as Boolean
    if (!exists) {
      throw MethodDoesNotExist()
    }
  }
}
