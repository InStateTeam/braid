package io.bluebank.braid.corda.serialisation.mixin

import com.fasterxml.jackson.databind.annotation.JsonAppend
import io.bluebank.braid.corda.serialisation.serializers.PRODUCT_TYPE_FIELD
import net.corda.core.contracts.PartyAndReference


@JsonAppend(attrs = [JsonAppend.Attr(value = PRODUCT_TYPE_FIELD)])
abstract class IssuedMixin<out P : Any>(val issuer: PartyAndReference, val product: P) {
}