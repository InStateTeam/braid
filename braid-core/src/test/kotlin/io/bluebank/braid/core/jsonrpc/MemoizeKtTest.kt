package io.bluebank.braid.core.jsonrpc

import org.junit.Test
import kotlin.test.assertEquals

class MemoizeTest {
  private var salt = 0L

  @Test
  fun `that we can memoize a two param function`() {
    val fn = this::sum.memoize()
    val result = fn(8, 7)
    assertEquals(fn(8, 7), result)
  }

  private fun sum(lhs: Int, rhs: Int) : Long {
    return (salt++ + lhs.toLong() + rhs.toLong())
  }
}