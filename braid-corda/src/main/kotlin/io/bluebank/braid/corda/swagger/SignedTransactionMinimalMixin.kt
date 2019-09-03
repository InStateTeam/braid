package io.bluebank.braid.corda.swagger

abstract class SignedTransactionMinimalMixin public constructor() {

  @com.fasterxml.jackson.annotation.JsonIgnore public abstract fun getNotaryChangeTx(): net.corda.core.transactions.NotaryChangeWireTransaction

}
