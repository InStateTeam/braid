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

import com.fasterxml.jackson.databind.JavaType
import io.bluebank.braid.corda.rest.docs.BraidSwaggerError
import io.swagger.converter.ModelConverter
import io.swagger.converter.ModelConverterContext
import io.swagger.models.Model
import io.swagger.models.ModelImpl
import io.swagger.models.properties.*
import io.swagger.util.Json
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.loggerFor
import java.lang.reflect.Type
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
class CustomModelConverterV2 : ModelConverter {
  companion object {
    private val log = loggerFor<CustomModelConverterV2>()
  }

  override fun resolveProperty(
    type: Type,
    context: ModelConverterContext,
    annotations: Array<out Annotation>?,
    chain: Iterator<ModelConverter>?
  ): Property? {
    return try {
      val jsonType = Json.mapper().constructType(type)
      when (jsonType) {
        null -> chain?.next()?.resolveProperty(type, context, annotations, chain)
        else -> {
          val clazz = jsonType.rawClass
          when {
            PublicKey::class.java.isAssignableFrom(clazz) -> publicKeyProperty()
            Class::class.java.isAssignableFrom(clazz) -> classProperty()
            SecureHash::class.java.isAssignableFrom(clazz) || SecureHash.SHA256::class.java.isAssignableFrom(clazz) -> secureHashProperty()
            X509Certificate::class.java.isAssignableFrom(clazz) -> x509CertificateProperty()
            CertPath::class.java.isAssignableFrom(clazz) -> certPathProperty()
            CordaX500Name::class.java.isAssignableFrom(clazz) -> cordaX500NameProperty()
            OpaqueBytes::class.java.isAssignableFrom(clazz) -> opaqueBytesProperty()
            Currency::class.java.isAssignableFrom(clazz) -> currencyProperty()
            Amount::class.java.isAssignableFrom(clazz) -> processAmountType(context, jsonType)
            Issued::class.java.isAssignableFrom(clazz) -> processIssuedType(context, annotations, jsonType)
            Throwable::class.java.isAssignableFrom(clazz) || BraidSwaggerError::class.java == clazz -> processThrowableType(
              context
            )
            else -> chain?.next()?.resolveProperty(type, context, annotations, chain)
          }
        }
      }
    } catch (e: Throwable) {
      log.error("Unable to parse or resolve type: $type", e)
      throw RuntimeException("Unable to resolve type:$type", e)
    }
  }

  private fun processThrowableType(
    context: ModelConverterContext
  ): Property? {
    context.defineModel(
      "Error", ModelImpl()
        .type("object")
        .property("message", StringProperty().description("the error message"))
        .property("type", StringProperty().description("the class of error being returned"))
    )
    return RefProperty("Error")
  }

  private fun processIssuedType(
    context: ModelConverterContext,
    annotations: Array<out Annotation>?,
    jsonType: JavaType
  ): RefProperty {
    // String and Currency get created as their own types
    val boundType = jsonType.bindings.getBoundType(0)
    return if (boundType != null && (boundType.rawClass == Currency::class.java || boundType.rawClass == String::class.java)
    ) {
      context.defineModel(
        "IssuedCurrency", ModelImpl()
          .type("object")
          .property("product", context.resolveProperty(boundType, annotations))
          .property("issuer", context.resolveProperty(PartyAndReference::class.java, annotations))
      )
      RefProperty("IssuedCurrency")
    } else {
      val model = ModelImpl()
        .type("object")
        .property("product", context.resolveProperty(boundType, annotations))
        .property("issuer", context.resolveProperty(PartyAndReference::class.java, annotations))
        .property("_productType", StringProperty().example("java.util.Currency"))
      context.defineModel("Issued", model)
      RefProperty("Issued")
    }
  }

  private fun processAmountType(
    context: ModelConverterContext,
    jsonType: JavaType
  ): RefProperty {
    // String and Currency get created as their own types
    val boundType = jsonType.bindings.getBoundType(0)
    return if (boundType != null && (boundType.rawClass == Currency::class.java)) {
      context.defineModel(
        "AmountCurrency", ModelImpl()
          .type("object")
          .property(
            "quantity",
            IntegerProperty().example(100).description("total amount in minor units")
          )
          .property("displayTokenSize", DecimalProperty().example("0.01"))
          .property("token", StringProperty().example("GBP"))
      )
      RefProperty("AmountCurrency")
    } else {
      val model = ModelImpl()
        .type("object")
        .property(
          "quantity",
          IntegerProperty().example(100).description("total amount in minor units")
        )
        .property("displayTokenSize", DecimalProperty().example("0.01"))
        .property("token", StringProperty().example("GBP"))
        .property(
          "_tokenType",
          StringProperty().example("net.corda.core.contracts.Issued")
        )
      context.defineModel("Amount", model)

      RefProperty("Amount")
    }
  }

  override fun resolve(
    type: Type?,
    context: ModelConverterContext?,
    chain: Iterator<ModelConverter>?
  ): Model? {
    return chain?.next()?.resolve(type, context, chain)
  }

  private fun publicKeyProperty() = StringProperty()
    .example("GfHq2tTVk9z4eXgyUuofmR16H6j7srXt8BCyidKdrZL5JEwFqHgDSuiinbTE")
    .description("Base 58 Encoded Public Key")

  private fun classProperty() = StringProperty()
    .example("java.lang.Object")
    .description("Java class name")

  private fun secureHashProperty() = StringProperty()
    .example("GfHq2tTVk9z4eXgyUuofmR16H6j7srXt8BCyidKdrZL5JEwFqHgDSuiinbTE")
    .description("Base 58 Encoded Secure Hash")

  private fun x509CertificateProperty() = ByteArrayProperty()
    .description("X509 encoded certificate")

  private fun certPathProperty() = ByteArrayProperty()
    .description("X509 encoded certificate PKI path")

  private fun cordaX500NameProperty() = StringProperty()
    .example("O=Bank A, L=London, C=GB")
    .description("CordaX500Name encoded Party")

  private fun opaqueBytesProperty() = StringProperty()
    .example("736F6D654279746573")
    .description("Hex encoded Byte Array")

  private fun currencyProperty() = StringProperty()
    .example("GBP")
    .description("3 digit ISO 4217 code of the currency")
}