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

import com.fasterxml.jackson.annotation.*
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria

@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@class")
@JsonSubTypes(
  JsonSubTypes.Type(value = QueryCriteria.VaultQueryCriteria::class),
  JsonSubTypes.Type(value = QueryCriteria.VaultCustomQueryCriteria::class),
  JsonSubTypes.Type(value = QueryCriteria.CommonQueryCriteria::class),
  JsonSubTypes.Type(value = QueryCriteria.LinearStateQueryCriteria::class),
  JsonSubTypes.Type(value = QueryCriteria.FungibleAssetQueryCriteria::class),
  JsonSubTypes.Type(value = QueryCriteria.FungibleStateQueryCriteria::class),
  JsonSubTypes.Type(value = QueryCriteria.AndComposition::class),
  JsonSubTypes.Type(value = QueryCriteria.OrComposition::class)
    )
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
abstract class QueryCriteriaMixin
@JsonCreator
constructor(@JsonProperty("status") status: Vault.StateStatus) {
}
