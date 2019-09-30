package io.bluebank.braid.corda.services.vault

import net.corda.core.contracts.ContractState
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort

data class VaultQuery(
   val criteria: QueryCriteria,
   val paging: PageSpecification,
   val sorting: Sort,
   val contractStateType:Class<ContractState>
)