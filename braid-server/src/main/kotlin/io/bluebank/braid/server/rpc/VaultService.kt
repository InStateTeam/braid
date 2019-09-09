package io.bluebank.braid.server.rpc

import io.bluebank.braid.server.domain.SimpleNodeInfo
import io.bluebank.braid.server.domain.toSimpleNodeInfo
import io.bluebank.braid.server.rpc.vault.VaultQuery
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import net.corda.core.contracts.ContractState
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.Sort
import javax.ws.rs.QueryParam

class VaultService(val rpc: RPCFactory){
  @ApiOperation(value = "Queries the vault")
  fun vaultQueryBy(
      @ApiParam(
      value = "Vault query parameters"
  ) vault: VaultQuery<ContractState>): Vault.Page<ContractState> {
    return rpc.validConnection().vaultQueryBy(vault.criteria,vault.paging,vault.sorting,vault.contractStateType)
  }

}