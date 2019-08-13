package io.bluebank.braid.server.rpc

import kotlin.reflect.KParameter
import kotlin.reflect.KType

class BodyKParameter(override val annotations: List<Annotation>,
                 override val index: Int,
                 override val isOptional: Boolean,
                 override val isVararg: Boolean,
                 override val kind: KParameter.Kind,
                 override val name: String?,
                 override val type: KType) : KParameter