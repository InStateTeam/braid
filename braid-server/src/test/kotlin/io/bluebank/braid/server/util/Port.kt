package io.bluebank.braid.server.util

import java.net.ServerSocket

fun getFreePort(): Int {
  return (ServerSocket(0)).use {
    it.localPort
  }
}
