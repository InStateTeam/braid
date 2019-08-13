package io.bluebank.braid.server.flow

import io.github.classgraph.ClassGraph
import net.corda.core.flows.StartableByRPC
import java.lang.Class.forName
import java.util.stream.Stream
import kotlin.reflect.KClass



class StartableByRPCFinder {
    companion object{
        fun rpcClasses(): Stream<KClass<*>>{
            return StartableByRPCFinder().findStartableByRPC().stream()
        }
    }

    fun findStartableByRPC(): List<KClass<*>> {
        val res = ClassGraph()
                .enableClassInfo()
                .enableAnnotationInfo()
                .addClassLoader(ClassLoader.getSystemClassLoader())
              //  .overrideClasspath(it.jarPath)
                .scan()
        return res.getClassesWithAnnotation(StartableByRPC::class.qualifiedName).names
                .map { forName(it).kotlin }
    }

}