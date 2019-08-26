package io.bluebank.braid.core.synth

import kotlin.reflect.KClassifier
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

class KTypeSynthetic(val clazz: Class<*>) :
        KType, KClassifier {
    override val arguments: List<KTypeProjection>
        get() = emptyList()
    override val classifier: KClassifier?
        get() = this
    override val isMarkedNullable: Boolean
        get() = false

}