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

import io.bluebank.braid.corda.rest.RestMounter
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.server.rpc.RPCFactory
import io.swagger.util.Json
import io.vertx.core.Vertx
import io.vertx.ext.web.impl.RouterImpl
import net.corda.core.internal.toTypedArray
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.RPCOps
import java.io.File
import java.net.URLClassLoader
import java.util.Arrays.stream


private val log = loggerFor<Braid>()

fun main(args: Array<String>) {

  if (args.size < 1) {
    throw IllegalArgumentException("Usage: BraidDocsMainKt <outputFileName> [<cordaAppJar1> <cordAppJar2> ....]")
  }

  val file = File(args[0])
  file.parentFile.mkdirs()

  val swaggerText = BraidDocsMain(classLoader(args)).swaggerText()
  log.info(swaggerText)
  file.writeText(swaggerText)
  log.info("wrote to:" + file.absolutePath)
}

fun classLoader(args: Array<String>): ClassLoader {
  val toArray = stream(args).skip(1).map { File(it).toURI().toURL() }.toTypedArray()
  return URLClassLoader(
          toArray,
          BraidDocsMain::class.java.getClassLoader()
  )
}

class BraidDocsMain(classLoader: ClassLoader? = ClassLoader.getSystemClassLoader()) {
  private var restMounter: RestMounter

  init {
    val restConfig = Braid().restConfig(RPCFactory("","",""), classLoader)
    val vertx = Vertx.vertx()
    restMounter = RestMounter(restConfig, RouterImpl(vertx), vertx)
  }

  fun swaggerText(): String {
    val swagger = restMounter.docsHandler.createSwagger()
    val swaggerText = Json.pretty().writeValueAsString(swagger)
    return swaggerText
  }
}
