package io.bluebank.jsonrpc

import io.vertx.core.Future


class MyService {
  fun add(lhs: Int, rhs: Int): Future<Int> {
    return Future.succeededFuture(lhs + rhs)
  }

  fun foo1(lhs: Int) {

  }

  fun foo2(rhs: Int) {

  }
}
