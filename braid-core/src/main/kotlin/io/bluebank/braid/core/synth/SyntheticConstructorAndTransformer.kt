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
import io.bluebank.braid.core.utils.tryWithClassLoader
import org.apache.commons.lang3.AnnotationUtils
import java.lang.reflect.*
import kotlin.reflect.*
import kotlin.reflect.full.createType

class SyntheticConstructorAndTransformer<K : Any, R>(
  internal val constructor: Constructor<K>,
  className: String = constructor.declaringClass.payloadClassName(),
  private val boundTypes: Map<Class<*>, Any>,
  private val classLoader: ClassLoader = ClassLoader.getSystemClassLoader(),
  private val transformer: (Array<Any?>) -> R,
  private val additionalParams: List<KParameter> = emptyList()
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

  private val payloadClass =
    acquirePayloadClass(constructor, boundTypes, classLoader, className)

  fun annotations(): Array<Annotation> = constructor.annotations
  fun invoke(vararg args: Any): R {
    // args is additionalParams (if any) followed by the single payload parameter
    val additionalParamArgs = args.take(additionalParams.size)
    // get the payload
    val constructorParam = args.last()

    return tryWithClassLoader(classLoader) {
      // get the constructor parameter values
      val parameterValues =
        constructor.parameters.map { getFieldValue(constructorParam, it) }
      // prepent the additional parameters (if any) again
      val allParams = (additionalParamArgs + parameterValues).toTypedArray()
      transformer(allParams)
    }
  }

  override val annotations: List<Annotation> = constructor.annotations.toList()
  override val isAbstract: Boolean = false
  override val isFinal: Boolean = true
  override val isOpen: Boolean = false
  override val name: String = className
  override val parameters: List<KParameter> = additionalParams + listOf(
    KParameterSynthetic(
      "payload",
      payloadClass
    )
  )

  // DocsHandler cant get java type from this  payloadClass.kotlin.createType()
  // Unit::class.createType()
  //  transformer.reflect()!!.returnType     // DocsHandler cant get java type from this
  override val returnType: KType =
    payloadClass.kotlin.createType() //KTypeSynthetic(payloadClass)       payloadClass.kotlin.createType()
  override val typeParameters: List<KTypeParameter> = emptyList()
  override val visibility: KVisibility? = KVisibility.PUBLIC
  override val isExternal: Boolean = false
  override val isInfix: Boolean = false
  override val isInline: Boolean = false
  override val isOperator: Boolean = false
  override val isSuspend: Boolean = false

  override fun call(vararg args: Any?): R {
    assert(args.size == additionalParams.size + 1 && args.all { it != null }) { "there should be only one non null parameter but instead got $args" }
    @Suppress("UNCHECKED_CAST") val nonNullArgs = args as Array<Any>
    return invoke(*nonNullArgs)
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
fun <T> Class<T>.preferredConstructor(): Constructor<T> {
  return this.constructors
    .filter { c -> !c.isSynthetic }
    .maxBy { c -> c.parameterCount } as Constructor<T>
}

fun ObjectMapper.decodeValue(json: String, fn: KFunction<*>): Any {
  return this.readValue(
    json,
    (fn.parameters.first().type.classifier as KClass<*>).javaObjectType
  )
}

fun <K : Any, R> trampoline(
  constructor: Constructor<K>,
  boundTypes: Map<Class<*>, Any>,
  className: String = constructor.declaringClass.payloadClassName(),
  classLoader: ClassLoader = ClassLoader.getSystemClassLoader(),
  additionalParams: List<KParameter> = emptyList(),
  transform: (Array<Any?>) -> R
): KFunction<R> {
  @Suppress("UNCHECKED_CAST")
  return SyntheticConstructorAndTransformer(
    constructor,
    className,
    boundTypes,
    classLoader,
    transform,
    additionalParams
  )
}

// https://stackoverflow.com/questions/16299717/how-to-create-an-instance-of-an-annotation#answer-57373532
inline fun <reified T : Any> KClass<T>.createAnnotationProxy(properties: Map<String, Any> = emptyMap()): T {
  val handler = object : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>): Any? {
      val annotation = proxy as Annotation
      val methodName = method.name
      when (methodName) {
        "toString" -> return AnnotationUtils.toString(annotation)
        "hashCode" -> return AnnotationUtils.hashCode(annotation)
        "equals" -> return AnnotationUtils.equals(annotation, args[0] as Annotation)
        "annotationType" -> return T::class.java
        else -> {
          if (!properties.containsKey(methodName)) {
            throw NoSuchMethodException(
              String.format(
                "Missing value for mocked annotation property '%s'. Pass the correct value in the 'properties' parameter",
                methodName
              )
            )
          }
          return properties[methodName]
        }
      }
    }
  }
  return Proxy.newProxyInstance(
    Thread.currentThread().contextClassLoader,
    arrayOf(T::class.java),
    handler
  ) as T
}
