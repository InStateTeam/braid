package io.bluebank.braid.corda.serialisation.mixin

import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
abstract class ThrowableMixin {

  //  @com.fasterxml.jackson.annotation.JsonIgnore abstract fun getCause(): Throwable?
  @com.fasterxml.jackson.annotation.JsonIgnore
  abstract fun getStackTrace(): Array<StackTraceElement>

  @com.fasterxml.jackson.annotation.JsonIgnore
  abstract fun getSuppressed(): List<Throwable>?
}