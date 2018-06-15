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

import io.bluebank.braid.corda.restafarian.docs.DocsHandler
import io.bluebank.braid.core.logging.loggerFor
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
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.sstore.LocalSessionStore
import kotlin.reflect.KCallable

enum class AuthSchema {
  None,
  Basic,
  Token
}

data class RestConfig(val serviceName: String = DEFAULT_SERVICE_NAME,
                      val description: String = DEFAULT_DESCRIPTION,
                      val hostAndPortUri: String = DEFAULT_HOST_AND_PORT_URI,
                      val apiPath: String = DEFAULT_API_PATH,
                      val swaggerPath: String = DEFAULT_SWAGGER_PATH,
                      val scheme: Scheme = DEFAULT_SCHEME,
                      val contact: Contact = DEFAULT_CONTACT,
                      val authSchema: AuthSchema = DEFAULT_AUTH_SCHEMA,
                      val authProvider: AuthProvider? = DEFAULT_AUTH_PROVIDER,
                      val initHandler: (Restafarian.(Router) -> Unit) = {}
) {
  companion object {
    const val DEFAULT_SERVICE_NAME = ""
    const val DEFAULT_DESCRIPTION = ""
    const val DEFAULT_HOST_AND_PORT_URI = "http://localhost:8080"
    const val DEFAULT_API_PATH = "/api/rest"
    const val DEFAULT_SWAGGER_PATH = "/"
    val DEFAULT_SCHEME = Scheme.HTTPS
    val DEFAULT_CONTACT : Contact = Contact().email("").name("").url("")
    val DEFAULT_AUTH_SCHEMA = AuthSchema.None
    val DEFAULT_AUTH_PROVIDER : AuthProvider? = null
  }
  fun withServiceName(value: String) = this.copy(serviceName = value)
  fun withDescription(value: String) = this.copy(description = value)
  fun withHostAndPortUri(value: String) = this.copy(description = value)
  fun withApiPath(value: String) = this.copy(apiPath = value)
  fun withSwaggerPath(value: String) = this.copy(swaggerPath = value)
  fun withScheme(value: Scheme) = this.copy(scheme = value)
  fun withAuthScheme(value: AuthSchema) = this.copy(authSchema = value)
  fun withContact(value: Contact) = this.copy(contact = value)
  fun withAuthProvider(value: AuthProvider?) = this.copy(authProvider = value)
  fun withInitHandler(value: Restafarian.(Router) -> Unit) = this.copy(initHandler = value)
}

class Restafarian(
  private val config: RestConfig = RestConfig(),
  private val router: Router,
  private val vertx: Vertx
) {

  companion object {
    private val log = loggerFor<Restafarian>()

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
      restMounter: Restafarian.(Router) -> Unit = {},
      vertx: Vertx
    ) {
      Restafarian(RestConfig(serviceName, description, hostAndPortUri, apiPath, swaggerPath, scheme, contact, authSchema, authProvider, restMounter), router, vertx)
    }

    fun mount(config: RestConfig, router: Router, vertx: Vertx) {
      Restafarian(config, router, vertx)
    }
  }

  private var groupName: String = ""

  private val path: String = if (config.apiPath.endsWith("/")) {
    config.apiPath.dropLast(1)
  } else {
    config.apiPath
  }

  private val swaggerPath: String = if (config.swaggerPath.endsWith("/")) {
    config.swaggerPath.dropLast(1)
  } else {
    config.swaggerPath
  }

  private val docsHandler = DocsHandler(
    serviceName = config.serviceName,
    description = config.description,
    basePath = config.hostAndPortUri + path,
    scheme = config.scheme,
    contact = config.contact,
    auth = when (config.authSchema) {
      AuthSchema.Basic -> BasicAuthDefinition()
      AuthSchema.Token -> ApiKeyAuthDefinition(HttpHeaders.AUTHORIZATION.toString(), In.HEADER)
      else -> null
    }
  )

  init {
    if (!config.apiPath.startsWith("/")) throw RuntimeException("path must begin with a /")
    mount(config.initHandler)
  }

  private fun mount(fn: Restafarian.(Router) -> Unit) {
    setupAuthRouting()
    // pass control to caller to setup rest bindings
    this.fn(router)
    log.info("REST end point bound to ${config.hostAndPortUri}$path")
    configureSwaggerAndStatic()
  }

  private fun configureSwaggerAndStatic() {
    // configure the swagger json
    router.get("$swaggerPath/swagger.json").handler(docsHandler)
    log.info("swagger json bound to ${config.hostAndPortUri}$swaggerPath/swagger.json")

    // and now for the swagger static
    val sh = StaticHandler.create("swagger").setCachingEnabled(false)
    router.get("/swagger/*").last().handler(sh)
    router.get("$swaggerPath/*").last().handler(sh)

    log.info("Swagger UI bound to ${config.hostAndPortUri}$swaggerPath")
  }

  private fun setupAuthRouting() {
    when (config.authSchema) {
      AuthSchema.Basic -> {
        verifyAuthProvider()
        router.route("$path/*").handler(CookieHandler.create())
        router.route("$path/*").handler(SessionHandler.create(LocalSessionStore.create(vertx)))
        router.route("$path/*").handler(UserSessionHandler.create(config.authProvider))
        router.route("$path/*").handler(BasicAuthHandler.create(config.authProvider))
      }
      AuthSchema.Token -> {
        verifyAuthProvider()
        router.route("$path/*").handler(JWTAuthHandler.create(config.authProvider as JWTAuth))
      }
      else -> {
        // don't add any auth provider
      }
    }
  }

  private fun verifyAuthProvider() {
    config.authProvider ?: throw RuntimeException("no auth provider given for auth schema ${config.authSchema}")
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

  @JvmName("getRaw")
  fun get(path: String, fn: RoutingContext.() -> Unit) {
    bind(HttpMethod.GET, path, fn)
  }

  @JvmName("putFuture")
  fun <Response> put(path: String, fn: KCallable<Future<Response>>) {
    bind(HttpMethod.PUT, path, fn)
  }

  fun <Response> put(path: String, fn: KCallable<Response>) {
    bind(HttpMethod.PUT, path, fn)
  }

  @JvmName("putRaw")
  fun put(path: String, fn: RoutingContext.() -> Unit) {
    bind(HttpMethod.PUT, path, fn)
  }

  fun <Response> post(path: String, fn: KCallable<Response>) {
    bind(HttpMethod.POST, path, fn)
  }

  @JvmName("postFuture")
  fun <Response> post(path: String, fn: KCallable<Future<Response>>) {
    bind(HttpMethod.POST, path, fn)
  }

  @JvmName("postRaw")
  fun post(path: String, fn: RoutingContext.() -> Unit) {
    bind(HttpMethod.POST, path, fn)
  }

  fun <Response> delete(path: String, fn: KCallable<Response>) {
    bind(HttpMethod.DELETE, path, fn)
  }

  @JvmName("deleteFuture")
  fun <Response> delete(path: String, fn: KCallable<Future<Response>>) {
    bind(HttpMethod.DELETE, path, fn)
  }

  @JvmName("deleteRaw")
  fun delete(path: String, fn: RoutingContext.() -> Unit) {
    bind(HttpMethod.DELETE, path, fn)
  }

  private fun bind(method: HttpMethod, path: String, fn: RoutingContext.() -> Unit) {
    router.route(method, "${this.path}$path").handler { it.fn() }
    docsHandler.add(groupName, method, path, fn)
  }

  private fun <Response> bind(method: HttpMethod, path: String, fn: KCallable<Future<Response>>) {
    router.route(method, "${this.path}$path").bind(fn)
    docsHandler.add(groupName, method, path, fn)
  }

  @JvmName("bindMethod0")
  private fun <Response> bind(method: HttpMethod, path: String, fn: KCallable<Response>) {
    router.route(method, "${this.path}$path").bind(fn)
    docsHandler.add(groupName, method, path, fn)
  }
}