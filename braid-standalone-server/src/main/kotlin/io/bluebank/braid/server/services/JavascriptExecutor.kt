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
package io.bluebank.braid.server.services

import io.bluebank.braid.core.jsonrpc.JsonRPCRequest
import io.bluebank.braid.core.jsonrpc.createJsonException
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.service.MethodDescriptor
import io.bluebank.braid.core.service.MethodDoesNotExist
import io.bluebank.braid.core.service.ServiceExecutor
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import rx.Observable
import rx.Observable.create
import java.nio.file.Paths
import javax.script.Invocable
import javax.script.ScriptEngine
import javax.script.ScriptEngineManager

class JavascriptExecutor(private val vertx: Vertx, private val name: String) : ServiceExecutor {
  companion object {
    private val logger = loggerFor<JavascriptExecutor>()
    private val SCRIPTS_PATH = "service-scripts"
    private val sem = ScriptEngineManager()
    private val SCRIPT_ENGINE_NAME = "nashorn"
    fun clearScriptsFolder(vertx: Vertx): Future<Unit> {
      val future = Future.future<Void>()

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

    fun queryServiceNames(vertx: Vertx): List<String> {
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

  override fun getStubs(): List<MethodDescriptor> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

  override fun invoke(request: JsonRPCRequest): Observable<Any> {
    return create { subscriber ->
      try {
        checkMethodExists(request.method)
        val params = if (request.params != null && request.params is List<*>) {
          val paramsList = request.params as List<*>
          paramsList.toTypedArray()
        } else {
          listOf(request.params).toTypedArray()
        }
        val result = invocable.invokeFunction(request.method, *params)
        subscriber.onNext(result)
        subscriber.onCompleted()
      } catch (err: MethodDoesNotExist) {
        subscriber.onError(err)
      } catch (err: Throwable) {
        subscriber.onError(err.createJsonException(request))
      }
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
      throw MethodDoesNotExist(methodName)
    }
  }

  fun deleteScript() {
    with(vertx.fileSystem()) {
      if (existsBlocking(scriptPath)) {
        deleteBlocking(scriptPath)
      }
    }
  }
}