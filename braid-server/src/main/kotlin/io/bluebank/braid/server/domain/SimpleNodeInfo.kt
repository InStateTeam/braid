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
package io.bluebank.braid.server.domain

import net.corda.core.identity.Party
import net.corda.core.node.NodeInfo
import net.corda.core.utilities.NetworkHostAndPort

data class SimpleNodeInfo(
  val addresses: List<NetworkHostAndPort>,
  val legalIdentities: List<Party>
)

fun NodeInfo.toSimpleNodeInfo(): SimpleNodeInfo {
  return SimpleNodeInfo(this.addresses, this.legalIdentities)
}