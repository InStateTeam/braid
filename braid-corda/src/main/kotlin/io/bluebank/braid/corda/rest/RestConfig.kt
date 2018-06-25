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
package io.bluebank.braid.corda.rest

import io.swagger.models.Contact
import io.swagger.models.Scheme
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.Router
import java.net.URI

data class RestConfig(val serviceName: String = DEFAULT_SERVICE_NAME,
                      val description: String = DEFAULT_DESCRIPTION,
                      val hostAndPortUri: String = DEFAULT_HOST_AND_PORT_URI,
                      val apiPath: String = DEFAULT_API_PATH,
                      val swaggerPath: String = DEFAULT_SWAGGER_PATH,
                      val contact: Contact = DEFAULT_CONTACT,
                      val authSchema: AuthSchema = DEFAULT_AUTH_SCHEMA,
                      internal val authProvider: AuthProvider? = DEFAULT_AUTH_PROVIDER,
                      val debugMode: Boolean = false,
                      val pathsInit: (RestMounter.(Router) -> Unit) = {}
) {
  companion object {
    const val DEFAULT_SERVICE_NAME = ""
    const val DEFAULT_DESCRIPTION = ""
    const val DEFAULT_HOST_AND_PORT_URI = "http://localhost:8080"
    const val DEFAULT_API_PATH = "/api/rest"
    const val DEFAULT_SWAGGER_PATH = "/"
    val DEFAULT_CONTACT: Contact = Contact().email("").name("").url("")
    val DEFAULT_AUTH_PROVIDER: AuthProvider? = null
    val DEFAULT_AUTH_SCHEMA = AuthSchema.None
  }

  val scheme: Scheme by lazy {
    when (URI.create(hostAndPortUri).scheme.toLowerCase()) {
      "https" -> Scheme.HTTPS
      "http" -> Scheme.HTTP
      else -> throw RuntimeException("unsupported protocol scheme for $hostAndPortUri")
    }
  }

  @Suppress("unused")
  fun withServiceName(value: String) = this.copy(serviceName = value)

  @Suppress("unused")
  fun withDescription(value: String) = this.copy(description = value)

  fun withHostAndPortUri(value: String) = this.copy(hostAndPortUri = value)
  @Suppress("unused")
  fun withApiPath(value: String) = this.copy(apiPath = value)

  @Suppress("unused")
  fun withSwaggerPath(value: String) = this.copy(swaggerPath = value)

  @Suppress("unused")
  fun withContact(value: Contact) = this.copy(contact = value)

  internal fun withAuth(value: AuthProvider?) = this.copy(authProvider = value)
  @Suppress("unused")
  fun withPaths(value: RestMounter.(Router) -> Unit) = this.copy(pathsInit = value)

  @Suppress("unused")
  fun withAuthSchema(authSchema: AuthSchema) = this.copy(authSchema = authSchema)

  @Suppress("unused")
  fun withDebugMode() = this.copy(debugMode = true)
}