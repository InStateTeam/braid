package io.bluebank.hermes.server

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class ServiceDescription(val name: String, val description: String)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class MethodDescription(val returnType: KClass<*> = Any::class, val description: String = "")