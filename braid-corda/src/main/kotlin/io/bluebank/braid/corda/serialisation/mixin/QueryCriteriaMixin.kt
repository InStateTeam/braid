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
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.ColumnPredicate
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
  JsonSubTypes.Type(value = QueryCriteria.OrComposition::class),
  JsonSubTypes.Type(value = QueryCriteria::class)
    )
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@io.swagger.v3.oas.annotations.media.Schema(
    type = "object",
    title = "QueryCriteria",
    discriminatorProperty = "@class",
    discriminatorMapping = [
        DiscriminatorMapping(value = ".QueryCriteria", schema = QueryCriteria::class),
        DiscriminatorMapping(value = ".QueryCriteria${'$'}VaultQueryCriteria", schema = QueryCriteria.VaultQueryCriteria::class),
        DiscriminatorMapping(value = ".QueryCriteria${'$'}VaultCustomQueryCriteria", schema = QueryCriteria.VaultCustomQueryCriteria::class),
        DiscriminatorMapping(value = ".QueryCriteria${'$'}CommonQueryCriteria", schema = QueryCriteria.CommonQueryCriteria::class),
        DiscriminatorMapping(value = ".QueryCriteria${'$'}LinearStateQueryCriteria", schema = QueryCriteria.LinearStateQueryCriteria::class),
        DiscriminatorMapping(value = ".QueryCriteria${'$'}FungibleAssetQueryCriteria", schema = QueryCriteria.FungibleAssetQueryCriteria::class),
        DiscriminatorMapping(value = ".QueryCriteria${'$'}AndComposition", schema = QueryCriteria.AndComposition::class),
        DiscriminatorMapping(value = ".QueryCriteria${'$'}OrComposition", schema = QueryCriteria.OrComposition::class)
    ],
    subTypes = [ColumnPredicate.AggregateFunction::class]
)
abstract class QueryCriteriaMixin
@JsonCreator
constructor(@JsonProperty("status") status: Vault.StateStatus) {
}
