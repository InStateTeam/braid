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
package io.bluebank.braid.corda.server.progress

import io.bluebank.braid.core.logging.loggerFor
import io.netty.handler.codec.http.HttpResponseStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.models.parameters.HeaderParameter
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext
import javax.ws.rs.HeaderParam
import javax.ws.rs.core.MediaType

class TrackerHandler(private val tracker: ProgressTrackerManager) {

  companion object {
    var log = loggerFor<TrackerHandler>()
  }

  @Operation(
    description = "Connect to the Progress Tracker of a flow. " +
      "This call will return chunked responses of all progress trackers for this flow",
    parameters = [Parameter(name="invocation-id", `in` = ParameterIn.HEADER, required = true)],
    responses = [ApiResponse(
      content = arrayOf(
                Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = Schema(implementation = Progress::class))))]
  )
  fun invoke(ctx: RoutingContext) {
    val invocationId = ctx.request().getHeader("invocation-id")
    val flowProgress = tracker.get(invocationId)

    when (flowProgress) {
      null -> replyNotFound(ctx)
      else -> {
        ctx.response()
          .setChunked(true)
          .putHeader("Content-Type","application/json; charset=utf-8")
          .putHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
          .putHeader("Pragma", "no-cache")
          .putHeader(HttpHeaders.EXPIRES, "0")

        flowProgress.progress.subscribe(
          { ctx.response().write(Json.encode(Progress().withInvocationId(invocationId).withStep(it))) },
          { replyWithError(it, ctx) },
          { ctx.response().end() }
        )
      }
    }
  }

  private fun replyWithError(it: Throwable?, ctx: RoutingContext) {
    log.error("Error from flow progress:", it);
    ctx.response()
      .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
      .end()
  }

  private fun replyNotFound(context: RoutingContext) {
    context.response()
      .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
      .end()
  }

}