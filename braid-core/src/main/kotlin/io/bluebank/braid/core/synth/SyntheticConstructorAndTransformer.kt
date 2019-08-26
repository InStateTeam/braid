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

import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.reflect.Constructor
import java.lang.reflect.Parameter
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.reflect

class SyntheticConstructorAndTransformer<K : Any, R>(
  internal val constructor: Constructor<K>,
  className: String = constructor.declaringClass.payloadClassName(),
  private val boundTypes: Map<Class<*>, Any>,
  classLoader: ClassLoader = ClassLoader.getSystemClassLoader(),
  private val transformer: (K) -> R
) : KFunction<R> {

  companion object {
    fun acquirePayloadClass(
      constructor: Constructor<*>,
      boundTypes: Map<Class<*>, Any>,
      classLoader: ClassLoader,
      className: String
    ): Class<*> {
      val parameters = constructor.parameters.filter { !boundTypes.contains(it.type) }
      return ClassFromParametersBuilder.acquireClass(
        parameters.toTypedArray(),
        classLoader,
        className
      )
    }
  }

  private val payloadClass = acquirePayloadClass(constructor, boundTypes, classLoader, className)
  fun annotations(): Array<Annotation> = constructor.annotations
  fun invoke(payload: Any): R {
    val parameterValues =
      constructor.parameters.map { getFieldValue(payload, it) }.toTypedArray()
    return transformer(constructor.newInstance(*parameterValues))
  }

  override val annotations: List<Annotation> = constructor.annotations.toList()
  override val isAbstract: Boolean = false
  override val isFinal: Boolean = true
  override val isOpen: Boolean = false
  override val name: String = className
  override val parameters: List<KParameter> = listOf(
    KParameterSynthetic(
      "payload",
      payloadClass
    )
  )

  // DocsHandler cant get java type from this  payloadClass.kotlin.createType()
  // Unit::class.createType()
  //  transformer.reflect()!!.returnType     // DocsHandler cant get java type from this
  override val returnType: KType = payloadClass.kotlin.createType() //KTypeSynthetic(payloadClass)       payloadClass.kotlin.createType()
  override val typeParameters: List<KTypeParameter> = emptyList()
  override val visibility: KVisibility? = KVisibility.PUBLIC
  override val isExternal: Boolean = false
  override val isInfix: Boolean = false
  override val isInline: Boolean = false
  override val isOperator: Boolean = false
  override val isSuspend: Boolean = false

  override fun call(vararg args: Any?): R {
    assert(args.size == 1 && args[0] != null) { "there should be only one non null parameter but instead got $args" }
    return invoke(args[0]!!)
  }

  override fun callBy(args: Map<KParameter, Any?>): R {
    assert(args.size == 1) { "there should be only one non null parameter but instead got $args" }
    return invoke(args.values.first()!!)
  }

  private fun getFieldValue(payload: Any, parameter: Parameter): Any? {
    return when {
      boundTypes.contains(parameter.type) -> boundTypes[parameter.type]
      else -> {
        payload.javaClass.fields.singleOrNull { it.name == parameter.name }?.get(payload)
          ?: error("field ${parameter.name} missing in payload $payload")
      }
    }
  }
}

@Suppress("UNCHECKED_CAST")
fun <T> Class<T>.preferredConstructor() : Constructor<T> {
  return this.constructors.first() as Constructor<T>
}

fun ObjectMapper.decodeValue(json: String, fn: KFunction<*>) : Any {
  return this.readValue(json, (fn.parameters.first().type.classifier as KClass<*>).javaObjectType)
}

fun <K: Any, R> trampoline(
  constructor: Constructor<K>,
  boundTypes: Map<Class<*>, Any>,
  className: String = constructor.declaringClass.payloadClassName(),
  classLoader: ClassLoader = ClassLoader.getSystemClassLoader(),
  transform: (K) -> R
): KFunction<R> {
  @Suppress("UNCHECKED_CAST")
  return SyntheticConstructorAndTransformer(constructor, className, boundTypes, classLoader, transform)
}


