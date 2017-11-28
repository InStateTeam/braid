package io.bluebank.hermes.corda.example

import io.bluebank.hermes.core.annotation.MethodDescription
import net.corda.core.node.services.Vault
import net.corda.finance.contracts.asset.Cash
import net.corda.node.services.api.ServiceHubInternal
import rx.Observable

class MyService(private val serviceHub: ServiceHubInternal) {

  @MethodDescription(description = "listens for cash state updates in the vault", returnType = Vault.Update::class)
  fun listenForCashUpdates() : Observable<Vault.Update<Cash.State>>{
    return serviceHub.database.transaction {
      serviceHub.vaultService.trackBy(Cash.State::class.java).updates
    }
  }
}