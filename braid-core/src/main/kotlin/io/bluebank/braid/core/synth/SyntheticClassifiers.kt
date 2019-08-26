package io.bluebank.braid.core.synth
//
//import org.jetbrains.kotlin.descriptors.annotations.Annotations
//import org.jetbrains.kotlin.types.*
//import kotlin.reflect.KClassifier
//import kotlin.reflect.KType
//import kotlin.reflect.KTypeProjection
//import kotlin.reflect.KVariance
//import kotlin.reflect.full.createType
//import kotlin.reflect.jvm.internal.KClassifierImpl
//import kotlin.reflect.jvm.internal.KTypeImpl
//import kotlin.reflect.jvm.internal.KotlinReflectionInternalError
//
//@SinceKotlin("1.1")
//fun KClassifier.createType(
//        arguments: List<KTypeProjection> = emptyList(),
//        nullable: Boolean = false,
//        annotations: List<Annotation> = emptyList()
//): KType {
//    val descriptor = (this as? KClassifierImpl)?.descriptor
//            ?: throw KotlinReflectionInternalError("Cannot create type for an unsupported classifier: $this (${this.javaClass})")
//
//    val typeConstructor = descriptor.typeConstructor
//    val parameters = typeConstructor.parameters
//    if (parameters.size != arguments.size) {
//        throw IllegalArgumentException("Class declares ${parameters.size} type parameters, but ${arguments.size} were provided.")
//    }
//
//    // TODO: throw exception if argument does not satisfy bounds
//
//    val typeAnnotations =
//            if (annotations.isEmpty()) Annotations.EMPTY
//            else Annotations.EMPTY // TODO: support type annotations
//
//    val kotlinType = createKotlinType(typeAnnotations, typeConstructor, arguments, nullable)
//
//    return KTypeImpl(kotlinType) {
//        TODO("Java type is not yet supported for types created with createType (classifier = $this)")
//    }
//}
//
//private fun createKotlinType(
//        typeAnnotations: Annotations, typeConstructor: TypeConstructor, arguments: List<KTypeProjection>, nullable: Boolean
//): SimpleType {
//    val parameters = typeConstructor.parameters
//    return KotlinTypeFactory.simpleType(typeAnnotations, typeConstructor, arguments.mapIndexed { index, typeProjection ->
//        val type = (typeProjection.type as KTypeImpl?)?.type
//        when (typeProjection.variance) {
//            KVariance.INVARIANT -> TypeProjectionImpl(Variance.INVARIANT, type!!)
//            KVariance.IN -> TypeProjectionImpl(Variance.IN_VARIANCE, type!!)
//            KVariance.OUT -> TypeProjectionImpl(Variance.OUT_VARIANCE, type!!)
//            null -> StarProjectionImpl(parameters[index])
//        }
//    }, nullable)
//}
//
///**
// * Creates an instance of [KType] with the given classifier, substituting all its type parameters with star projections.
// * The resulting type is not marked as nullable and does not have any annotations.
// *
// * @see [KClassifier.createType]
// */
//@SinceKotlin("1.1")
//val KClassifier.starProjectedType: KType
//    get() {
//        val descriptor = (this as? KClassifierImpl)?.descriptor
//                ?: return createType()
//
//        val typeParameters = descriptor.typeConstructor.parameters
//        if (typeParameters.isEmpty()) return createType() // TODO: optimize, get defaultType from ClassDescriptor
//
//        return createType(typeParameters.map { KTypeProjection.STAR })
//    }
