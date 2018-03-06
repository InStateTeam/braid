/*
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

package io.bluebank.braid.core.restafarian

import io.bluebank.braid.core.http.bind
import io.vertx.core.Future
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
import kotlin.reflect.KCallable


class Restafarian(serviceName: String = "", description: String = "", basePath: String = "http://localhost:8080", private val publicPath: String = "/api", private val swaggerUIMount: String = "/", val router: Router) {
  private var groupName : String = ""
  private val docsHandler = DocsHandler(serviceName = serviceName, description = description, basePath = basePath, publicPath = publicPath)

  companion object {
    fun mount(serviceName: String = "", description: String = "", basePath: String = "http://localhost:8080", publicPath: String = "/api", swaggerUIMount: String = "/", router: Router, fn: Restafarian.(Router) -> Unit) {
      Restafarian(serviceName, description, basePath, publicPath, swaggerUIMount, router).mount(fn)
    }
  }

  fun mount(fn: Restafarian.(Router) -> Unit) {
    this.fn(router)
    router.get(publicPath).handler(docsHandler)
    router.get("$swaggerUIMount*").handler(StaticHandler.create("swagger").setCachingEnabled(false))
  }

  fun group(groupName: String, fn: () -> Unit) {
    this.groupName = groupName
    fn()
  }

  @JvmName("getFuture")
  fun <Response> get(path: String, fn: KCallable<Future<Response>>) {
    bind(HttpMethod.GET, path, fn)
  }

  fun <Response> get(path: String, fn: KCallable<Response>) {
    bind(HttpMethod.GET, path, fn)
  }

  fun <Response> post(path: String, fn: KCallable<Response>) {
    bind(HttpMethod.POST, path, fn)
  }

  @JvmName("postFuture")
  fun <Response> post(path: String, fn: KCallable<Future<Response>>) {
    bind(HttpMethod.POST, path, fn)
  }

  fun <Response> delete(path: String, fn: KCallable<Response>) {
    bind(HttpMethod.DELETE, path, fn)
  }

  @JvmName("deleteFuture")
  fun <Response> delete(path: String, fn: KCallable<Future<Response>>) {
    bind(HttpMethod.DELETE, path, fn)
  }

  private fun <Response> bind(method: HttpMethod, path: String, fn: KCallable<Future<Response>>) {
    router.route(method, "$publicPath$path").bind(fn)
    docsHandler.add(groupName, method, "$publicPath$path", fn)
  }

  @JvmName("bindMethod0")
  private fun <Response> bind(method: HttpMethod, path: String, fn: KCallable<Response>) {
    router.route(method, path).bind(fn)
    docsHandler.add(groupName, method, path, fn)
  }

}