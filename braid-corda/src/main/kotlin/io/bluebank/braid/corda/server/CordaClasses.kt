package io.bluebank.braid.corda.server

import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import net.corda.core.CordaInternal
import net.corda.core.flows.FlowInitiator
import net.corda.core.flows.FlowLogic
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.ProgressTracker
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

     return res.allClasses.asSequence()
        .filter {  isCordaSerializedClass(it)   }
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

  private fun isCordaSerializable(it: ClassInfo):Boolean =
      it.hasAnnotation(CordaSerializable::class.java.name)
          || (it.superclass != null && isCordaSerializable(it.superclass))
          || it.interfaces.stream()
              .filter{isCordaSerializable(it)}
              .findFirst()
              .isPresent()
        
}