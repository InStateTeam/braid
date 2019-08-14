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
package io.bluebank.braid.core.synth

import io.bluebank.braid.core.logging.loggerFor
import org.objectweb.asm.ClassWriter
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Parameter

/**
 * Builder for synthetic POJO with just a single constructor and fields
 * from parameters of an existing constructor or method
 */
data class ClassFromParametersSynthesizer(
  val parameters: List<Parameter> = emptyList(),
  val className: String = ""
) {
  companion object {
    private val logger = loggerFor<ClassFromParametersSynthesizer>()

    @JvmStatic
    fun acquireClass(
      method: Method,
      classLoader: ClassLoader,
      className: String = method.declaringClass.payloadClassName()
    ) = acquireClass(method.parameters, classLoader, className)

    @JvmStatic
    fun acquireClass(
      constructor: Constructor<*>,
      classLoader: ClassLoader,
      className: String = constructor.declaringClass.payloadClassName()
    ) = acquireClass(constructor.parameters, classLoader, className)

    @JvmStatic
    fun acquireClass(
      parameters: Array<Parameter>,
      classLoader: ClassLoader,
      className: String
    ): Class<*> {
      return classLoader.lazyAcquire(className) {
        ClassFromParametersSynthesizer()
          .withParameters(parameters)
          .withClassName(className)
          .buildAndInject(classLoader)
      }
    }

    /**
     * attempts to load the class - if it fails builds the type, injects it
     * @return the class matching [className]
     */
    private fun ClassLoader.lazyAcquire(className: String, fn: () -> Class<*>) : Class<*> {
      return try {
        loadClass(className)
      } catch(err: ClassNotFoundException) {
        return fn()
      }
    }

    /**
     * access to the [ClassLoader.defineClass] method - used to deploy the class bytecode
     */
    private val defineClassMethod: Method by lazy {
      val cls = Class.forName("java.lang.ClassLoader")
      cls.getDeclaredMethod(
        "defineClass",
        *arrayOf<Class<*>>(
          String::class.java,
          ByteArray::class.java,
          Int::class.java,
          Int::class.java
        )
      ).apply { isAccessible = true }
    }
  }

  fun withConstructor(constructor: Constructor<*>): ClassFromParametersSynthesizer {
    val name = classNameOrDefault { constructor.declaringClass.payloadClassName() }
    return copy(parameters = constructor.parameters.toList(), className = name)
  }

  fun withMethod(method: Method): ClassFromParametersSynthesizer {
    val name = classNameOrDefault { method.declaringClass.payloadClassName() }
    return copy(parameters = method.parameters.toList(), className = name)
  }

  fun withParameters(parameters: Array<Parameter>) = copy(parameters = parameters.toList())

  fun withClassName(name: String) = this.copy(className = name)

  /**
   * builds the bytecode of the class and injects it into [classLoader]
   */
  fun buildAndInject(classLoader: ClassLoader) = classLoader.inject(build())

  /**
   * builds the bytecode of the class
   * @return the bytecode array
   */
  fun build(): ByteArray {
    assert(className.isNotBlank()) { "class name was not set" }
    val jvmByteCodeName = className.replace('.', '/');
    return ClassWriter(0).apply {
      declareSimplePublicClass(jvmByteCodeName)
      addFields(parameters.toTypedArray())
      writeDefaultConstructor()
      visitEnd()
    }.toByteArray()
  }

  private fun ClassLoader.inject(bytes: ByteArray): Class<*> {
    assert(className.isNotBlank()) { "class name not set" }
    return try {
      loadClass(className).also {
        logger.warn("Payload type $className already declared")
      }
    } catch (error: ClassNotFoundException) {
      defineClassMethod.invoke(this, className, bytes, 0, bytes.size)
      return loadClass(className)
    }
  }

  private fun classNameOrDefault(fn: () -> String) =
    when {
      className.isBlank() -> fn()
      else -> className
    }
}

const val PAYLOAD_CLASS_SUFFIX = "Payload"
fun Class<*>.payloadClassName() = this.name + PAYLOAD_CLASS_SUFFIX