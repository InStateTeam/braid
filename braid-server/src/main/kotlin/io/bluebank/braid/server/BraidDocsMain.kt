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

import io.bluebank.braid.corda.server.Braid
import io.bluebank.braid.corda.server.BraidDocsMain
import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.utils.toJarsClassLoader
import io.bluebank.braid.core.utils.tryWithClassLoader
import java.io.File

private val log = loggerFor<Braid>()

fun main(args: Array<String>) {
  if (args.isEmpty()) {
    throw IllegalArgumentException("Usage: BraidDocsMainKt <outputFileName> [<cordaAppJar1> <cordAppJar2> ....]")
  }

  val file = File(args[0])
  file.parentFile.mkdirs()
  val jars = args.toList().drop(1)
  val swaggerText = generateSwaggerText(jars)
  file.writeText(swaggerText)
  log.info("wrote to: ${file.absolutePath}")
}

internal fun generateSwaggerText(jars: List<String>): String {
  val cordappsClassLoader = jars.toJarsClassLoader()
  // we call so as to initialise model converters etc before replacing the context class loader
  Braid.init()
  val swaggerText = tryWithClassLoader(cordappsClassLoader) {
    BraidDocsMain().swaggerText()
  }
  return swaggerText
}
