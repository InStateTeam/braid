/**
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bluebank.braid.core.socket

import java.net.ServerSocket

fun findFreePort(): Int {
  return ServerSocket(0).use {
    it.localPort
  }
}

fun findFreePorts(count: Int): IntArray {
  return (1..count).toList().map { ServerSocket(0) }.map { it.use { it.localPort } }
    .toIntArray()
}

fun findConsequtiveFreePorts(count: Int): Int {
  return generateSequence { findFreePort() }.filter { isPortRangeFree(it..it + count) }
    .first()
}

private fun isPortRangeFree(range: IntRange): Boolean {
  return range.map {
    try {
      ServerSocket(it).use {
        it.localPort
      }
    } catch (error: RuntimeException) {
      0
    }
  }.filter { it == 0 }.count() == 0
}