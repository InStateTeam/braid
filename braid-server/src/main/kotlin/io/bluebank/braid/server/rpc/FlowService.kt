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
package io.bluebank.braid.server.rpc

import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.server.Braid
import io.bluebank.braid.server.domain.SimpleNodeInfo
import io.bluebank.braid.server.domain.toSimpleNodeInfo
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import java.util.stream.Collectors.toList
import javax.ws.rs.QueryParam

class FlowService(val rpc: CordaRPCOps) {
    companion object {
        private val log = loggerFor<Braid>()
    }

    @ApiOperation(value = "Retrieves a list of callable flows. Example [\"net.corda.core.flows.ContractUpgradeFlow\$Authorise\"]")
    fun flows(): List<String> {
        return rpc.registeredFlows()
    }

    @ApiOperation(value = "Retrieves a list of flow details")
    fun flowDetails(@ApiParam(value = "Flow name", example = "net.corda.core.flows.ContractUpgradeFlow\$Authorise") @QueryParam(value = "flow") flow:String): String {
        return rpc.registeredFlows().filter { it.equals(flow) }.first()
    }

}