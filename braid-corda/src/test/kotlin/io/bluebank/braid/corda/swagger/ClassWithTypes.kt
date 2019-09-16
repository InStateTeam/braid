package io.bluebank.braid.corda.swagger

import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.WireTransaction
import net.corda.core.utilities.OpaqueBytes
import java.util.*

data class ClassWithTypes(
    val currency: Currency,
    val amountCurrency: Amount<Currency>
    , val amountString: Amount<String>
    , val amount: Amount<Any>
    , val party: Party
    , val bytes: OpaqueBytes
    , val hash: SecureHash
    , val issuedString: Issued<String>
    , val issuedCurrency: Issued<Currency>
    , val issued: Issued<CustomModelConvertersV2Test.IssuedType>
    , val signed: SignedTransaction
    , val wire: WireTransaction
    , val clazz: Class<*>
)