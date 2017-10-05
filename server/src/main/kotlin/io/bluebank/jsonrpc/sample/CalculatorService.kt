package io.bluebank.jsonrpc.sample

import io.bluebank.jsonrpc.server.JsonRPCService
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture


@JsonRPCService("calculator", "a simple calculator")
class CalculatorService {
  fun add(lhs: Int, rhs: Int): Future<Int> {
    return succeededFuture(lhs + rhs)
  }

  fun subtract(lhs: Int, rhs: Int): Future<Int> {
    return succeededFuture(lhs - rhs)
  }
}
