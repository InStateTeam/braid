package io.bluebank.braid.core.jsonrpc

import java.util.concurrent.ConcurrentHashMap

class Memoize2<in T1, in T2, out R>(val f: (T1, T2) -> R) : (T1, T2) -> R {
  private val values = ConcurrentHashMap<Pair<T1, T2>, R>()
  override fun invoke(x: T1, y: T2) = values.computeIfAbsent(x to y) { f(x, y) }
}

fun <T1, T2, R> ((T1, T2) -> R).memoize(): (T1, T2) -> R = Memoize2(this)