package io.bluebank.braid.server.flow

import net.corda.core.flows.FlowLogic
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.allSupertypes

fun KClass<*>.flowLogicType() : KType {
    val type = this.allSupertypes.stream()
            .filter { it.classifier?.equals(FlowLogic::class)!! }
            .findFirst()
            .get()
            .arguments
            .get(0)
            .type!!
    return type
}