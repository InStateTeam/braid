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

import io.bluebank.braid.corda.rest.docs.DocsHandler
import io.bluebank.braid.corda.rest.docs.DocsHandlerV2
import io.bluebank.braid.corda.rest.docs.v3.DocsHandlerV3
import io.swagger.models.auth.ApiKeyAuthDefinition
import io.swagger.models.auth.BasicAuthDefinition
import io.swagger.models.auth.In
import io.swagger.models.auth.SecuritySchemeDefinition
import io.vertx.core.http.HttpHeaders

class DocsHandlerFactory(
    val config: RestConfig,
    val path: String = config.apiPath.trim().dropWhile { it == '/' }.dropLastWhile { it == '/' }
) {

  fun createDocsHandler(): DocsHandler {
    when (config.openApiVersion) {
      3 -> return DocsHandlerV3(
          swaggerInfo = config.swaggerInfo,
          debugMode = config.debugMode,
        basePath = "${config.hostAndPortUri}/$path",
          auth = null ///todo auth
      )
      2 -> return DocsHandlerV2(
          swaggerInfo = config.swaggerInfo,
          scheme = config.scheme,
          debugMode = config.debugMode,
        basePath = "${config.hostAndPortUri}/$path",
          auth = getSecuritySchemeDefinition()
      )
      else -> TODO("Unknown OpenAPI version ${config.openApiVersion}")
    }
  }

  private fun getSecuritySchemeDefinition(): SecuritySchemeDefinition? {
    return when (config.authSchema) {
      AuthSchema.Basic -> {
        BasicAuthDefinition()
      }
      AuthSchema.Token -> {
        ApiKeyAuthDefinition(HttpHeaders.AUTHORIZATION.toString(), In.HEADER)
      }
      else -> {
        null
      }
    }
  }
}