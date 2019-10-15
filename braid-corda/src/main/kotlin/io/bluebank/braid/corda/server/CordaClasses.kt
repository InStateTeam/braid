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
import net.corda.core.flows.FlowInitiator
import net.corda.core.flows.FlowLogic
import net.corda.core.serialization.CordaSerializable
import net.corda.core.serialization.SerializeAsToken
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.ByteSequence
import net.corda.core.utilities.ProgressTracker
import net.corda.node.internal.AbstractNode
import rx.Observable

class CordaClasses {
  companion object{
    val isFunctionName = Regex(".*\\$[a-z].*\\$[0-9]+.*")::matches
    val isCompanionClass = Regex(".*\\$" + "Companion")::matches
    val isKotlinFileClass = Regex(".*Kt$")::matches
  }

  fun readCordaClasses(): List<Class<out Any>> {
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
            "net.corda.node.services.statemachine",
            "net.corda.node.migration",
            "net.corda.node.internal"
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
        .blacklistClasses(ByteSequence::class.java.name)
        .scan()

     return res.allClasses.asSequence()
        .filter {  isCordaSerializedClass(it)   }
        .filter {  !isSingletonSerializeAsToken(it)   }
        .map { it.loadClass() }
        .toList()
  }

  fun isCordaSerializedClass(it: ClassInfo): Boolean {
    return isCordaSerializable(it) &&
        !it.hasAnnotation(CordaInternal::class.java.name) &&
        !it.isInterface &&
        !it.isAbstract &&
        !it.extendsSuperclass(FlowLogic::class.java.name) &&
        !it.extendsSuperclass(FlowInitiator::class.java.name) &&
        !it.extendsSuperclass(Throwable::class.java.name) &&
        !isFunctionName(it.name) &&
        !isCompanionClass(it.name) &&
        !isKotlinFileClass(it.name)
  }

  private fun isSingletonSerializeAsToken(type: ClassInfo):Boolean =
    type.implementsInterface(SerializeAsToken::class.java.name)

  private fun isCordaSerializable(type: ClassInfo):Boolean =
      type.hasAnnotation(CordaSerializable::class.java.name)
          || (type.superclass != null && isCordaSerializable(type.superclass))
          || type.interfaces.stream()
              .filter{isCordaSerializable(it)}
              .findFirst()
              .isPresent()
        
}