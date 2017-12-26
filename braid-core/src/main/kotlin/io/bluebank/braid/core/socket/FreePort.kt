package io.bluebank.braid.core.socket

import java.net.ServerSocket

fun findFreePort() : Int {
  return ServerSocket(0).use {
    it.localPort
  }
}