package io.bluebank.braid.corda.serialisation

abstract class WireTransactionMixin public constructor() {
  @com.fasterxml.jackson.annotation.JsonIgnore public abstract fun getAvailableComponentGroups(): kotlin.collections.List<kotlin.Any>

  @com.fasterxml.jackson.annotation.JsonIgnore public abstract fun getAvailableComponentHashes(): kotlin.collections.List<net.corda.core.crypto.SecureHash>

  @com.fasterxml.jackson.annotation.JsonIgnore public abstract fun getAvailableComponents(): kotlin.collections.List<kotlin.Any>

  @com.fasterxml.jackson.annotation.JsonIgnore public abstract fun getMerkleTree(): net.corda.core.crypto.MerkleTree

  @com.fasterxml.jackson.annotation.JsonIgnore public abstract fun getOutputStates(): kotlin.collections.List<net.corda.core.contracts.ContractState>
}