package io.bluebank.braid.corda.swagger

import io.swagger.converter.ModelConverters
import io.swagger.jackson.AbstractModelConverter
import io.swagger.jackson.ModelResolver
import io.vertx.core.json.Json
import java.security.PublicKey

/**
 * To be used when calling BraidCordaJacksonInit.init()
 */
object CustomModelConverters {
    init {
        ModelConverters.getInstance().addConverter(CustomModelConverter())
    }

    fun init() {

    }
}