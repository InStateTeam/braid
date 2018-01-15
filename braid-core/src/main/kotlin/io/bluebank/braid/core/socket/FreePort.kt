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

fun findConsequtiveFreePorts(count: Int) : Int {
  return generateSequence { findFreePort() }.filter { isPortRangeFree(it .. it + count) }.first()
}

private fun isPortRangeFree(range: IntRange) : Boolean {
  return range.map {
    try {
    ServerSocket(it).use {
      it.localPort
    }
  } catch(error: RuntimeException) {
    0
  } }.filter { it == 0 }.count() == 0
}