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
package io.bluebank.braid.corda.services.vault

import io.bluebank.braid.corda.server.rpc.RPCFactory
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
    val vaultQueryBy = rpc.validConnection()
        .vaultQueryBy(vault.criteria, vault.paging, vault.sorting, vault.contractStateType)
    return vaultQueryBy
  }



  @ApiOperation(value = "Queries the vault for contract states of the supplied type")
  fun vaultQuery(
      @QueryParam(value = "contract-state-type")
      @ApiParam(
      value = "Vault query by contract state type"
  ) type: Class<ContractState>?): Vault.Page<ContractState> {
    val vaultQuery = rpc.validConnection().vaultQuery(type ?: ContractState::class.java)
    return vaultQuery
  }

}