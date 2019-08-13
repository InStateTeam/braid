package io.bluebank.braid.server

import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import java.util.*

@StartableByRPC
class BraidTestFlow(amount: Amount<Currency>,issuerBankPartyRef: net.corda.core.utilities.OpaqueBytes, notary: net.corda.core.identity.Party)

    : FlowLogic<SignedTransaction>() {

    override fun call(): SignedTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}