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
import io.bluebank.braid.core.utils.tryWithClassLoader
import io.bluebank.braid.server.rpc.RPCFactory.Companion.createRpcFactoryStub
import io.bluebank.braid.server.util.toCordappsClassLoader
import io.github.classgraph.ClassGraph
import io.vertx.core.Vertx
import io.vertx.ext.web.impl.RouterImpl
import net.corda.core.CordaInternal
import net.corda.core.flows.FlowInitiator
import net.corda.core.flows.FlowLogic
import java.io.File

private val log = loggerFor<Braid>()

fun main(args: Array<String>) {
  if (args.isEmpty()) {
    throw IllegalArgumentException("Usage: BraidDocsMainKt <outputFileName> <swaggerVersion 2,3> [<cordaAppJar1> <cordAppJar2> ....]")
  }

  val file = File(args[0])
  val swaggerVersion = Integer.parseInt(args[1])
  val jarUrls = args.toList().drop(2)

  file.parentFile.mkdirs()
  // we call so as to initialise model converters etc before replacing the context class loader
  Braid.init()
  tryWithClassLoader(jarUrls.toCordappsClassLoader()) {
    val swaggerText = BraidDocsMain().swaggerText(swaggerVersion)
    file.writeText(swaggerText)
  }
  log.info("wrote to: ${file.absolutePath}")
}

class BraidDocsMain() {
  fun swaggerText(swaggerVersion: Int = 2): String {
    val restConfig = Braid().restConfig(createRpcFactoryStub(), swaggerVersion)

    // todo replace with DocsHandlerFactory(restConfig).createDocsHandler()
    val vertx = Vertx.vertx()
    val restMounter = RestMounter(restConfig, RouterImpl(vertx), vertx)
    val classes = readCordaClasses()

    classes.forEach { restMounter.docsHandler.addType(it) }
    return restMounter.docsHandler.swagger()
  }

  private fun readCordaClasses(): List<Class<out Any>> {
    val res = ClassGraph()
      .enableClassInfo()
      .enableAnnotationInfo()
      .addClassLoader(ClassLoader.getSystemClassLoader())
      .whitelistPackages("net.corda")
      .blacklistPackages(
        "net.corda.internal",
        "net.corda.client",
        "net.corda.core.internal",
        "net.corda.nodeapi.internal",
        "net.corda.serialization.internal",
        "net.corda.testing",
        "net.corda.common.configuration.parsing.internal",
        "net.corda.finance.internal",
        "net.corda.common.validation.internal",
        "net.corda.client.rpc.internal",
        "net.corda.core.cordapp"
      )
      .scan()

    val isFunctionName = Regex(".*\\$[a-z].*\\$[0-9]+.*")::matches
    val isCompanionClass = Regex(".*\\$" + "Companion")::matches
    val isKotlinFileClass = Regex(".*Kt$")::matches
    return res.allClasses
      .filter {
        !it.hasAnnotation(CordaInternal::class.java.name) &&
          !it.isInterface &&
          !it.isAbstract &&
          !it.extendsSuperclass(FlowLogic::class.java.name) &&
          !it.extendsSuperclass(FlowInitiator::class.java.name) &&
          !isFunctionName(it.name) &&
          !isCompanionClass(it.name) &&
          !isKotlinFileClass(it.name)
      }
      .map { it.loadClass() }
  }
}
