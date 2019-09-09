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
package io.bluebank.braid.corda.serialisation

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.bluebank.braid.corda.swagger.SignedTransactionMinimalMixin
import io.bluebank.braid.core.json.BraidJacksonInit
import io.vertx.core.json.Json
import net.corda.client.jackson.JacksonSupport
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.NodeInfo
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TraversableTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes
import java.security.PublicKey
import java.util.Arrays.asList

/**
 * If you add to this file, please also add to CustomModelConverter forcorrect swagger generation
 */
object BraidCordaJacksonInit {
  init {
    BraidJacksonInit.init()
    // we reuse the jackson support from corda, replacing those that are not flexible enough for
    // dynamic languages
    @Suppress("DEPRECATION") val sm = SimpleModule("io.swagger.util.DeserializationModule")
            .addAbstractTypeMapping(AbstractParty::class.java, Party::class.java)

// we won't use the party serliazers due to the way they require a specialised ObjectMapper!
//          .addSerializer(AnonymousParty::class.java, JacksonSupport.AnonymousPartySerializer)
//          .addDeserializer(AnonymousParty::class.java, JacksonSupport.AnonymousPartyDeserializer)
//          .addSerializer(Party::class.java, JacksonSupport.PartySerializer)
//          .addDeserializer(Party::class.java, JacksonSupport.PartyDeserializer)
//          .addDeserializer(AbstractParty::class.java, JacksonSupport.PartyDeserializer)
            .addSerializer(SecureHash::class.java, SecureHashSerializer)
            .addSerializer(SecureHash.SHA256::class.java, SecureHashSerializer)
            .addDeserializer(SecureHash::class.java, SecureHashDeserializer())
            .addDeserializer(SecureHash.SHA256::class.java, SecureHashDeserializer())

            // For ed25519 pubkeys
            // TODO: Fix these
//          .addSerializer(EdDSAPublicKey::class.java, JacksonSupport.PublicKeySerializer)
//          .addDeserializer(EdDSAPublicKey::class.java, JacksonSupport.PublicKeyDeserializer)

            // For NodeInfo
            // TODO this tunnels the Kryo representation as a Base58 encoded string. Replace when RPC supports this.
            .addSerializer(NodeInfo::class.java, JacksonSupport.NodeInfoSerializer)
            .addDeserializer(NodeInfo::class.java, JacksonSupport.NodeInfoDeserializer)

            // For OpaqueBytes
            .addDeserializer(OpaqueBytes::class.java, OpaqueBytesDeserializer())
            .addSerializer(OpaqueBytes::class.java, OpaqueBytesSerializer())

            // For X.500 distinguished names
            .addDeserializer(CordaX500Name::class.java, JacksonSupport.CordaX500NameDeserializer)
            .addSerializer(CordaX500Name::class.java, JacksonSupport.CordaX500NameSerializer)

            // Mixins for transaction types to prevent some properties from being serialized
            .setMixInAnnotation(SignedTransaction::class.java, JacksonSupport.SignedTransactionMixin::class.java)
            // Caused by: io.vertx.core.json.EncodeException: Failed to encode as JSON: net.corda.core.transactions.WireTransaction cannot be cast to net.corda.core.transactions.NotaryChangeWireTransaction (through reference chain: net.corda.finance.flows.AbstractCashFlow$Result["stx"]->net.corda.core.transactions.SignedTransaction["notaryChangeTx"])
            .setMixInAnnotation(WireTransaction::class.java, WireTransactionMixin::class.java)
            .setMixInAnnotation(TraversableTransaction::class.java, TraversableTransactionMixin::class.java)

            .addSerializer(PublicKey::class.java, PublicKeySerializer())
            .addDeserializer(PublicKey::class.java, PublicKeyDeserializer())
            // For Amount
            // we do not use the Corda amount serialisers
            .addSerializer(Amount::class.java, AmountSerializer())
            .addDeserializer(Amount::class.java, AmountDeserializer())
            //     .addSerializer(Currency::class.java, CurrencySerializer())          likely to cause issues distinuishing strings from currency in Amount and Issuer class
            //     .addDeserializer(Currency::class.java, CurrencyDeserializer())
            .addSerializer(Issued::class.java, IssuedSerializer())
            .addDeserializer(Issued::class.java, IssuedDeserializer())

    asList(Json.mapper, Json.prettyMapper)
            .forEach {
              it.registerModule(sm)
              .registerModule(ParameterNamesModule())
              .registerModule(Jdk8Module())
              .registerModule(JavaTimeModule())
    };
  }

  fun init() {
    // automatically initialise the static constructor
  }
}

