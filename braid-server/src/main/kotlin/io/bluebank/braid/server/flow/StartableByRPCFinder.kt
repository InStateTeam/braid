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
package io.bluebank.braid.server.flow

import io.github.classgraph.ClassGraph
import net.corda.core.flows.StartableByRPC
import java.lang.Class.forName
import java.util.stream.Stream
import kotlin.reflect.KClass

class StartableByRPCFinder {
  companion object {
    fun rpcClasses(classLoader: ClassLoader? = ClassLoader.getSystemClassLoader()): Stream<KClass<*>> {
      return StartableByRPCFinder().findStartableByRPC(classLoader).stream()
    }
  }

  fun findStartableByRPC(classLoader: ClassLoader?): List<KClass<*>> {
    val res = ClassGraph()
      .enableClassInfo()
      .enableAnnotationInfo()
      .addClassLoader(classLoader)
      //  .overrideClasspath(it.jarPath)
      .scan()
    return res.getClassesWithAnnotation(StartableByRPC::class.qualifiedName).names
      .map { forName(it).kotlin }
  }

}