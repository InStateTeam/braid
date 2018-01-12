package io.bluebank.braid.core.socket

import java.net.ServerSocket

fun findFreePort() : Int {
  return ServerSocket(0).use {
    it.localPort
  }
}

fun findFreePorts(count: Int) : IntArray {
  return (1..count).map { ServerSocket(0) }.map { it.use { it.localPort } }.toIntArray()
}