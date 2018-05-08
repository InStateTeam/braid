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
package io.bluebank.braid.corda.restafarian

import io.swagger.models.Contact
import io.swagger.models.Scheme
import io.swagger.models.auth.ApiKeyAuthDefinition
import io.swagger.models.auth.BasicAuthDefinition
import io.swagger.models.auth.In
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.LocalSessionStore
import kotlin.reflect.KCallable

enum class AuthSchema {
  None,
  Basic,
  Token
}

class Restafarian(
    serviceName: String = "",
    description: String = "",
    hostAndPortUri: String = "http://localhost:8080",
    apiPath: String = "/api",
    swaggerPath: String = "/",
    scheme: Scheme = Scheme.HTTPS,
    contact: Contact = Contact().name("").email("").url(""),
    private val authSchema: AuthSchema = AuthSchema.None,
    private val authProvider: AuthProvider? = null,
    private val router: Router,
    private val vertx: Vertx
) {
  init {
    if (!apiPath.startsWith("/")) throw RuntimeException("path must begin with a /")
  }

  private var groupName : String = ""

  private val path: String = if (apiPath.endsWith("/")) {
    apiPath.dropLast(1)
  } else {
    apiPath
  }

  private val swaggerPath: String = if (swaggerPath.endsWith("/")) {
    swaggerPath.dropLast(1)
  } else {
    swaggerPath
  }

  private val docsHandler = DocsHandler(
      serviceName = serviceName,
      description = description,
      basePath = hostAndPortUri + path,
      scheme = scheme,
      contact = contact,
      auth = when(authSchema) {
        AuthSchema.Basic -> BasicAuthDefinition()
        AuthSchema.Token -> ApiKeyAuthDefinition(HttpHeaders.AUTHORIZATION.toString(), In.HEADER)
        else -> null
      }
  )

  companion object {
    fun mount(
        serviceName: String = "",
        description: String = "",
        hostAndPortUri: String = "http://localhost:8080",
        apiPath: String = "/api",
        swaggerPath: String = "/",
        router: Router,
        scheme: Scheme = Scheme.HTTPS,
        contact: Contact = Contact().email("").name("").url(""),
        authSchema: AuthSchema = AuthSchema.None,
        authProvider: AuthProvider? = null,
        vertx: Vertx,
        fn: Restafarian.(Router) -> Unit
    ) {
      Restafarian(serviceName, description, hostAndPortUri, apiPath, swaggerPath, scheme, contact, authSchema, authProvider, router, vertx).mount(fn)
    }
  }

  fun mount(fn: Restafarian.(Router) -> Unit) {
    setupAuthRouting()

    // pass control to caller to setup rest bindings
    this.fn(router)

    configureSwaggerAndStatic()
  }

  private fun configureSwaggerAndStatic() {
    // configure the swagger json
    router.get("$swaggerPath/swagger.json").handler(docsHandler)

    // and now for the swagger static
    val sh = StaticHandler.create("swagger").setCachingEnabled(false)
    router.get("/swagger/*").last().handler(sh)
    router.get("$swaggerPath/*").last().handler(sh)
  }

  private fun setupAuthRouting() {
    when (authSchema) {
      AuthSchema.Basic -> {
        verifyAuthProvider()
        router.route("$path/*").handler(CookieHandler.create())
        router.route("$path/*").handler(SessionHandler.create(LocalSessionStore.create(vertx)))
        router.route("$path/*").handler(UserSessionHandler.create(authProvider))
        router.route("$path/*").handler(BasicAuthHandler.create(authProvider))
      }
      AuthSchema.Token -> {
        verifyAuthProvider()
        router.route("$path/*").handler(JWTAuthHandler.create(authProvider as JWTAuth))
      }
      else -> {
        // don't add any auth provider
      }
    }
  }

  private fun verifyAuthProvider() {
    authProvider ?: throw RuntimeException("no auth provider given for auth schema $authSchema")
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
    router.route(method, "${this.path}$path").bind(fn)
    docsHandler.add(groupName, method, "${this.path}$path", fn)
  }

  @JvmName("bindMethod0")
  private fun <Response> bind(method: HttpMethod, path: String, fn: KCallable<Response>) {
    router.route(method, "${this.path}$path").bind(fn)
    docsHandler.add(groupName, method, path, fn)
  }
}