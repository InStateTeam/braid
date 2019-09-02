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
package io.bluebank.braid.server

import com.nhaarman.mockito_kotlin.mock
import io.bluebank.braid.corda.rest.RestMounter
import io.bluebank.braid.core.logging.loggerFor
import io.swagger.util.Json
import io.vertx.core.Vertx
import io.vertx.ext.web.impl.RouterImpl
import java.io.File

private val log = loggerFor<Braid>()

fun main(args: Array<String>) {

  if (args.size != 1) {
    throw IllegalArgumentException("Usage: BraidDocsMainKt <outputFileName>")
  }

  val restConfig = Braid().restConfig(mock())

  val vertx = Vertx.vertx()
  val restMounter = RestMounter(restConfig, RouterImpl(vertx), vertx)
  val swagger = restMounter.docsHandler.createSwagger()
  val swaggerText = Json.pretty().writeValueAsString(swagger)

  log.info(swaggerText)
  val file = File(args[0])
  file.parentFile.mkdirs()
  file.writeText(swaggerText)
  log.info("wrote to:" + file.absolutePath)
}

