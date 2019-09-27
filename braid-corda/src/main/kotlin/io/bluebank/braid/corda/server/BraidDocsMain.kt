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
package io.bluebank.braid.corda.server

import io.bluebank.braid.corda.rest.RestMounter
import io.bluebank.braid.corda.server.rpc.RPCFactory
import io.github.classgraph.ClassGraph
import io.vertx.core.Vertx
import io.vertx.ext.web.impl.RouterImpl
import net.corda.core.CordaInternal
import net.corda.core.flows.FlowInitiator
import net.corda.core.flows.FlowLogic
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.contextLogger
import rx.Observable
import java.util.concurrent.CountDownLatch

class BraidDocsMain() {
  companion object {
    private val log = contextLogger()
  }
  /**
   * @param openApiVersion - 2 or 3
   */
  fun swaggerText(openApiVersion: Int): String {
    val restConfig =
      BraidCordaStandaloneServer().restConfig(RPCFactory.createRpcFactoryStub()).withOpenApiVersion(openApiVersion)
    val vertx = Vertx.vertx()
    return try {
      val restMounter = RestMounter(restConfig, RouterImpl(vertx), vertx)
      val classes = readCordaClasses()
      classes.forEach { restMounter.docsHandler.addType(it) }
      restMounter.docsHandler.swagger()
    } finally {
      log.info("shutting down Vertx")
      val done = CountDownLatch(1)
      vertx.close {
        log.info("vertx shutdown")
        done.countDown()
      }
      done.await()
    }
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
          "net.corda.core.cordapp",
          "net.corda.core.messaging",
          "net.corda.node.services.statemachine"
        )
        .blacklistClasses(ProgressTracker::class.java.name)
        .blacklistClasses(ProgressTracker.Change::class.java.name)
        .blacklistClasses(ProgressTracker.Change.Position::class.java.name)
        .blacklistClasses(ProgressTracker.Change.Rendering::class.java.name)
        .blacklistClasses(ProgressTracker.Change.Structural::class.java.name)
        .blacklistClasses(ProgressTracker.STARTING::class.java.name)
        .blacklistClasses(ProgressTracker.UNSTARTED::class.java.name)
        .blacklistClasses(ProgressTracker.Step::class.java.name)
        .blacklistClasses(Observable::class.java.name)
        .scan()

    val isFunctionName = Regex(".*\\$[a-z].*\\$[0-9]+.*")::matches
    val isCompanionClass = Regex(".*\\$" + "Companion")::matches
    val isKotlinFileClass = Regex(".*Kt$")::matches
    return res.allClasses.asSequence()
        .filter {
              it.hasAnnotation(CordaSerializable::class.java.name) &&
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
        .toList()
  }
}
