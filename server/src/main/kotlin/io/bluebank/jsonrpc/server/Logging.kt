package io.bluebank.jsonrpc.server

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T: Any> loggerFor() : Logger {
  return LoggerFactory.getLogger(T::class.java)
}