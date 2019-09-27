package io.bluebank.braid.corda.serialisation.mixin

abstract class CordaThrowableMixin : ThrowableMixin() {
  @get:com.fasterxml.jackson.annotation.JsonIgnore
  abstract var originalExceptionClassName: String?
  @get:com.fasterxml.jackson.annotation.JsonIgnore
  abstract var originalMessage: String?
  @get:com.fasterxml.jackson.annotation.JsonIgnore
  abstract var localizedMessage: String?
}