package io.bluebank.braid.corda.rest.docs

import io.vertx.core.Handler
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.RoutingContext
import java.lang.reflect.Type
import kotlin.reflect.KCallable

interface DocsHandler : Handler<RoutingContext>{
  fun <Response> add(
      groupName: String,
      protected: Boolean,
      method: HttpMethod,
      path: String,
      handler: KCallable<Response>
  )

  fun add(
      groupName: String,
      protected: Boolean,
      method: HttpMethod,
      path: String,
      handler: (RoutingContext) -> Unit
  )

  fun addType(type: Type)

  fun swagger():String
}