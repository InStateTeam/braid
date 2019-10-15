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
import net.corda.core.contracts.ContractState
import net.corda.core.flows.ContractUpgradeFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.internal.toMultiMap
import kotlin.reflect.KClass

/**
 * Retrieves a set of jar module names that are cordapps. The technique
 */
class CordappScanner(private val classLoader: ClassLoader) {
  fun cordapps(): List<String> {
    return lazyCordapps
  }

  fun flowsForCordapp(cordapp: String): List<String>? {
    return flowsByCordapp[cordapp]?.map { it.name } ?: emptyList()
  }

  fun cordappAndFlowList(): List<Pair<String, KClass<out Any>>> {
    return flowClassesByCordapp
  }


  fun contractStates(): List<KClass<out Any>> {
    return contractStateClasses
  }

  private val classGraph by lazy {
    ClassGraph()
      .addClassLoader(classLoader)
      .enableClassInfo()
      .enableAnnotationInfo()
      .addClassLoader(Thread.currentThread().contextClassLoader)
        .blacklistClasses(ContractUpgradeFlow.Initiate::class.java.name)
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

  private val flowClassesByCordapp by lazy {
    flows.map { it.classpathElementFile.nameWithoutExtension.removeVersion() to classLoader.loadClass(it.name).kotlin }
      .sortedBy { it.first }
  }

  private val contractStateClasses by lazy {
    contractStates.map { classLoader.loadClass(it.name).kotlin }
  }

}

