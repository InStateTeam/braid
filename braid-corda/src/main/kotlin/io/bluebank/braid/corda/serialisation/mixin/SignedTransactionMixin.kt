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
package io.bluebank.braid.corda.serialisation.mixin

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.serialization.SerializedBytes
import net.corda.core.transactions.CoreTransaction
import net.corda.core.transactions.NotaryChangeWireTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction

interface SignedTransactionMixin {
  @get:JsonIgnore
  val notaryChangeTx: NotaryChangeWireTransaction

  @get:JsonIgnore
  val isNotaryChangeTransaction: Boolean

  @get:JsonIgnore
  val tx: WireTransaction

  @get:JsonIgnore
  val id: SecureHash

  @get:JsonIgnore
  val coreTransaction: CoreTransaction

  @get:JsonIgnore
  val inputs: List<*>

  @get:JsonIgnore
  val notary: Party?

  @get:JsonIgnore
  val networkParametersHash: SecureHash?

  @get:JsonIgnore
  val requiredSigningKeys: Set<*>?

}