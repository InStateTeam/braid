package io.bluebank.hermes.sample

import io.bluebank.hermes.server.MethodDescription
import io.bluebank.hermes.server.ServiceDescription
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture


@ServiceDescription("calculator", "a simple calculator")
class CalculatorService {

  @MethodDescription(description = "add two ints", returnType = Int::class)
  fun add(lhs: Int, rhs: Int): Future<Int> {
    return succeededFuture(lhs + rhs)
  }

  @MethodDescription(description = "subtract the second int from the first", returnType = Int::class)
  fun subtract(lhs: Int, rhs: Int): Future<Int> {
    return succeededFuture(lhs - rhs)
  }

  @MethodDescription(description = "multiply two ints", returnType = Int::class)
  fun multiply(lhs: Int, rhs: Int) : Int {
    return lhs * rhs
  }

  fun divide(lhs: Double, rhs: Double) : Double {
    return lhs / rhs
  }

  @MethodDescription(description = "multiply two complex numbers")
  fun multiplyComplex(lhs: ComplexNumber, rhs: ComplexNumber) : ComplexNumber {
    return ComplexNumber(lhs.x * rhs.x - lhs.y * rhs.y, lhs.x * rhs.y + lhs.y * rhs.x)
  }
}

class ComplexNumber(val x: Double, val y: Double)
