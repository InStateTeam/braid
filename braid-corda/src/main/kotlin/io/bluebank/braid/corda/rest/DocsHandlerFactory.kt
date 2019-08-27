package io.bluebank.braid.corda.rest

import io.bluebank.braid.corda.rest.docs.DocsHandler
import io.swagger.models.auth.ApiKeyAuthDefinition
import io.swagger.models.auth.BasicAuthDefinition
import io.swagger.models.auth.In
import io.swagger.models.auth.SecuritySchemeDefinition
import io.vertx.core.http.HttpHeaders

class DocsHandlerFactory(val config: RestConfig, val path: String = config.apiPath.trim().dropWhile { it == '/' }.dropLastWhile { it == '/' }) {

    fun createDocsHandler(): DocsHandler {
        return DocsHandler(
                serviceName = config.serviceName,
                description = config.description,
                basePath = "${config.hostAndPortUri}/$path/",
                scheme = config.scheme,
                contact = config.contact,
                auth = getSecuritySchemeDefinition(),
                debugMode = config.debugMode
        )
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