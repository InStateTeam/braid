package io.bluebank.braid.corda.services

import io.bluebank.braid.corda.server.rpc.RPCFactory
import io.bluebank.braid.corda.services.vault.VaultQuery
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import net.corda.core.contracts.ContractState
import net.corda.core.node.services.Vault
import javax.ws.rs.QueryParam

class VaultService(val rpc: RPCFactory){
  @ApiOperation(value = "Queries the vault")
  fun vaultQueryBy(
      @ApiParam(
      value = "Vault query parameters"
  ) vault: VaultQuery): Vault.Page<ContractState> {
    return rpc.validConnection()
        .vaultQueryBy(vault.criteria,vault.paging,vault.sorting,vault.contractStateType)
  }

  @ApiOperation(value = "Queries the vault for contract states of the supplied type")
  fun vaultQuery(
      @QueryParam(value = "contract-state-type")
      @ApiParam(
      value = "Vault query by contract state type"
  ) vault: Class<ContractState>): Vault.Page<ContractState> {
    return rpc.validConnection().vaultQuery(vault)
  }

}