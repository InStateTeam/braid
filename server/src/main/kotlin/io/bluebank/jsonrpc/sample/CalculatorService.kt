package io.bluebank.jsonrpc.sample

import io.bluebank.jsonrpc.server.JsonRPCReturns
import io.bluebank.jsonrpc.server.JsonRPCService
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture


@JsonRPCService("calculator", "a simple calculator")
class CalculatorService {
  @JsonRPCReturns("int")
  fun add(lhs: Int, rhs: Int): Future<Int> {
    return succeededFuture(lhs + rhs)
  }

  @JsonRPCReturns("int")
  fun subtract(lhs: Int, rhs: Int): Future<Int> {
    return succeededFuture(lhs - rhs)
  }

  fun multiply(lhs: Int, rhs: Int) : Int {
    return lhs * rhs
  }

  fun divide(lhs: Double, rhs: Double) : Double {
    return lhs / rhs
  }
}
