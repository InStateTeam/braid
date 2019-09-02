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
package io.bluebank.braid.corda.swagger

import io.swagger.converter.ModelConverter
import io.swagger.converter.ModelConverterContext
import io.swagger.models.Model
import io.swagger.models.ModelImpl
import io.swagger.models.properties.*
import io.swagger.util.Json
import net.corda.core.contracts.Amount
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.OpaqueBytes
import java.lang.reflect.Type
import java.security.PublicKey
import java.util.*


/**
 * From https://github.com/swagger-api/swagger-core/issues/1167
 *
 * to be used when calling BraidCordaJacksonInit.init()
 *
 */
class CustomModelConverter : ModelConverter {


    override fun resolveProperty(type: Type, context: ModelConverterContext, annotations: Array<out Annotation>, chain: MutableIterator<ModelConverter>): Property? {
        val jsonType = Json.mapper().constructType(type)
        if (jsonType != null) {
            val clazz = jsonType.getRawClass()
            if (PublicKey::class.java.isAssignableFrom(clazz)) {
                return StringProperty()
                        .example("GfHq2tTVk9z4eXgyUuofmR16H6j7srXt8BCyidKdrZL5JEwFqHgDSuiinbTE")
                        .description("Base 58 Encoded Public Key")
            }
            if (SecureHash::class.java.isAssignableFrom(clazz) || SecureHash.SHA256::class.java.isAssignableFrom(clazz)) {
                return StringProperty()
                        .example("GfHq2tTVk9z4eXgyUuofmR16H6j7srXt8BCyidKdrZL5JEwFqHgDSuiinbTE")
                        .description("Base 58 Encoded Secure Hash")
            }
            if (CordaX500Name::class.java.isAssignableFrom(clazz)) {
                return StringProperty()
                        .example("O=Bank A, L=London, C=GB")
                        .description("CordaX500Name encoded Party")
            }
            if (OpaqueBytes::class.java.isAssignableFrom(clazz)) {
                return StringProperty()
                        .example("736F6D654279746573")
                        .description("Hex encoded Byte Array")
            }

            if (Amount::class.java.isAssignableFrom(clazz)) {
                // String and Currency get created as their own types
                val boundType = jsonType.bindings.getBoundType(0)
                if (boundType.rawClass.equals(Currency::class.java) || boundType.rawClass.equals(String::class.java)) {
                    context.defineModel("AmountCurrency", ModelImpl()
                            .type("object")
                            .property("quantity", IntegerProperty().example(100).description("total amount in minor units"))
                            .property("displayTokenSize", DecimalProperty().example("0.01"))
                            .property("token", StringProperty().example("GBP")))
                    return RefProperty("AmountCurrency")
                } else {
                    val model = ModelImpl()
                            .type("object")
                            .property("quantity", IntegerProperty().example(100).description("total amount in minor units"))
                            .property("displayTokenSize", DecimalProperty().example("0.01"))
                            .property("token", StringProperty().example("GBP"))
                            .property("_tokenType", StringProperty().example("net.corda.core.contracts.Issued"))
                    context.defineModel("Amount",model)

                    return RefProperty("Amount")

                }
            }

        }




        if (chain.hasNext()) {
            return chain.next().resolveProperty(type, context, annotations, chain);
        } else {
            return null;
        }
    }


    override fun resolve(type: Type, context: ModelConverterContext, chain: Iterator<ModelConverter>): Model? {
        return if (chain.hasNext()) {
            chain.next().resolve(type, context, chain)
        } else {
            null
        }
    }

}