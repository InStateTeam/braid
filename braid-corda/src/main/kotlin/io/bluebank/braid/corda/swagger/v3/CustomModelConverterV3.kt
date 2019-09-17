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
package io.bluebank.braid.corda.swagger.v3

import io.swagger.util.Json
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverter
import io.swagger.v3.oas.models.media.*
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.loggerFor
import java.security.PublicKey
import java.security.cert.CertPath
import java.security.cert.X509Certificate
import java.util.*

/**
 * From https://github.com/swagger-api/swagger-core/issues/1167
 *
 * to be used when calling BraidCordaJacksonInit.init()
 *
 */
class CustomModelConverterV3 : ModelConverter {

  companion object {
    private val log = loggerFor<CustomModelConverterV3>()
  }

  override fun resolve(type: AnnotatedType, context: io.swagger.v3.core.converter.ModelConverterContext, chain: MutableIterator<ModelConverter>): Schema<*> {
    try {
      val jsonType = Json.mapper().constructType(type.type)
      if (jsonType != null) {
        val clazz = jsonType.rawClass
        if (PublicKey::class.java.isAssignableFrom(clazz)) {
          return StringSchema()
              .example("GfHq2tTVk9z4eXgyUuofmR16H6j7srXt8BCyidKdrZL5JEwFqHgDSuiinbTE")
              .description("Base 58 Encoded Public Key")
        }
        if (Class::class.java.isAssignableFrom(clazz)) {
          return StringSchema()
              .example("java.lang.Object")
              .description("Java class name")
        }
        if (SecureHash::class.java.isAssignableFrom(clazz) || SecureHash.SHA256::class.java.isAssignableFrom(clazz)) {
          return StringSchema()
              .example("GfHq2tTVk9z4eXgyUuofmR16H6j7srXt8BCyidKdrZL5JEwFqHgDSuiinbTE")
              .description("Base 58 Encoded Secure Hash")
        }
        if (CordaX500Name::class.java.isAssignableFrom(clazz)) {
          return StringSchema()
              .example("O=Bank A, L=London, C=GB")
              .description("CordaX500Name encoded Party")
        }
        if (X509Certificate::class.java.isAssignableFrom(clazz)) {
          return ByteArraySchema()
              .description("X509 encoded certificate")
        }
        if (CertPath::class.java.isAssignableFrom(clazz)) {
          return ByteArraySchema()
              .description("X509 encoded certificate PKI path")
        }
        if (OpaqueBytes::class.java.isAssignableFrom(clazz)) {
          return StringSchema()
              .example("736F6D654279746573")
              .description("Hex encoded Byte Array")
        }
        if (Currency::class.java.isAssignableFrom(clazz)) {
          return StringSchema()
              .example("GBP")
              .description("3 digit ISO 4217 code of the currency")
        }

        if (Amount::class.java.isAssignableFrom(clazz)) {
          // String and Currency get created as their own types
          val boundType = jsonType.bindings.getBoundType(0)
          if (boundType != null && (boundType.rawClass == Currency::class.java)
          ) {
            return ObjectSchema()
                .name("AmountCurrency")
                .addProperties("quantity", IntegerSchema()
                    .example(100)
                    .description("total amount in minor units")
                )
                .addProperties("displayTokenSize", NumberSchema().example("0.01"))
                .addProperties("token", StringSchema().example("GBP"))
                .addRequiredItem("quantity")
                .addRequiredItem("displayTokenSize")
                .addRequiredItem("token")

          } else {
            return ObjectSchema()
                .name("Amount")
                .addProperties("quantity", IntegerSchema()
                    .example(100)
                    .description("total amount in minor units")
                )
                .addProperties("displayTokenSize", NumberSchema().example("0.01"))
                .addProperties("token", StringSchema().example("GBP"))
                .addProperties("_tokenType", StringSchema()
                    .example("net.corda.core.contracts.Issued")
                )
                .addRequiredItem("quantity")
                .addRequiredItem("displayTokenSize")
                .addRequiredItem("token")
          }
        }
        if (Issued::class.java.isAssignableFrom(clazz)) {
          // String and Currency get created as their own types
          val boundType = jsonType.bindings.getBoundType(0)
          if (boundType != null && (boundType.rawClass == Currency::class.java || boundType.rawClass == String::class.java)
          ) {
            return ObjectSchema()
                .name("IssuedCurrency")
                .addProperties("issuer", context.resolve(AnnotatedType(PartyAndReference::class.java)))
                .addProperties("product", context.resolve(AnnotatedType(boundType)))
                .addRequiredItem("issuer")
                .addRequiredItem("product")
          } else {
            return ObjectSchema()
                .name("Issued")
                .addProperties("issuer", context.resolve(AnnotatedType(PartyAndReference::class.java)))
                .addProperties("product", context.resolve(AnnotatedType(boundType)))
                .addProperties("_productType", StringSchema().example("java.util.Currency"))
                .addRequiredItem("issuer")
                .addRequiredItem("product")
                .addRequiredItem("_productType")
          }
        }
      }
    } catch (e: Throwable) {
      log.error("Unable to parse: $type", e)
    }

    return try {
      chain.next().resolve(type, context, chain)
    } catch (e: Exception) {
      throw RuntimeException("Unable to resolve type:$type",e )
    }
  }


}