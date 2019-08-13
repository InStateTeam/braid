package io.bluebank.braid.server.rpc

import kotlin.reflect.KClassifier
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

class BodyKType(override val arguments: List<KTypeProjection>,
                override val classifier: KClassifier?,
                override val isMarkedNullable: Boolean) : KType