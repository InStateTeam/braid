package io.bluebank.braid.corda.serialisation

abstract class TraversableTransactionMixin public constructor() {
  @com.fasterxml.jackson.annotation.JsonIgnore public abstract fun getAvailableComponentGroups(): kotlin.collections.List<kotlin.Any>

}