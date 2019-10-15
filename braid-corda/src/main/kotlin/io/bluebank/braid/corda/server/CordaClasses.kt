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

import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import net.corda.core.CordaInternal
import net.corda.core.contracts.ContractState
import net.corda.core.flows.ContractUpgradeFlow
import net.corda.core.flows.FlowInitiator
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.internal.toMultiMap
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.ProgressTracker

/**
 * Retrieves a set of jar module names that are cordapps
 */
class CordaClasses(private val classLoader: ClassLoader = Thread.currentThread().contextClassLoader) {
  fun cordapps(): List<String> {
    return lazyCordapps
  }

  fun flowsForCordapp(cordapp: String): List<String>? {
    return flowsByCordapp[cordapp]?.map { it.name } ?: emptyList()
  }

  val flowClassesByCordapp by lazy {
    flows.map { it.classpathElementFile.nameWithoutExtension.removeVersion() to classLoader.loadClass(it.name).kotlin }
      .sortedBy { it.first }
  }

  val contractStateClasses by lazy {
    contractStates.map { classLoader.loadClass(it.name).kotlin }
  }


  private val classGraph by lazy {
    ClassGraph()
      .addClassLoader(classLoader)
      .enableClassInfo()
      .enableAnnotationInfo()
      .addClassLoader(Thread.currentThread().contextClassLoader)
      .blacklistClasses(ContractUpgradeFlow.Initiate::class.java.name)
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
        "net.corda.node.services.statemachine",
        "net.corda.node.migration",
        "net.corda.node.internal",
        "net.corda.core.flows"
      )
      .scan()
  }

  private val contractStates by lazy {
    classGraph.getClassesImplementing(ContractState::class.java.name)
  }

  private val flows by lazy {
    classGraph.getClassesWithAnnotation(StartableByRPC::class.qualifiedName)
  }

  private val lazyCordapps by lazy {
    (contractStates + flows).map { it.classpathElementFile.nameWithoutExtension.removeVersion() }.distinct().sorted()
  }

  private val flowsByCordapp by lazy {
    flows.map { it.classpathElementFile.nameWithoutExtension.removeVersion() to it }.toMultiMap()
  }


  companion object {
    val isFunctionName = Regex(".*\\$[a-z].*\\$[0-9]+.*")::matches
    val isCompanionClass = Regex(".*\\$" + "Companion")::matches
    val isKotlinFileClass = Regex(".*Kt$")::matches

    private fun isCordaSerializable(type: ClassInfo): Boolean =
      type.hasAnnotation(CordaSerializable::class.java.name)
        || (type.superclass != null && isCordaSerializable(type.superclass))
        || type.interfaces.stream()
        .filter { isCordaSerializable(it) }
        .findFirst()
        .isPresent

    internal fun isCordaSerializedClass(it: ClassInfo): Boolean =
      isCordaSerializable(it) &&
        !it.hasAnnotation(CordaInternal::class.java.name) &&
        !it.isInterface &&
        !it.isAbstract &&
        !it.extendsSuperclass(ProgressTracker.Step::class.java.name) &&
        !it.extendsSuperclass(FlowLogic::class.java.name) &&
        !it.extendsSuperclass(FlowInitiator::class.java.name) &&
        !it.extendsSuperclass(Throwable::class.java.name) &&
        !isFunctionName(it.name) &&
        !isCompanionClass(it.name) &&
        !isKotlinFileClass(it.name) &&
        it.name != ProgressTracker.Step::class.java.name
  }
}

