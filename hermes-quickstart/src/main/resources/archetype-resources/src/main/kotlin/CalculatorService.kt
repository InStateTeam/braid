package ${package}

import io.bluebank.hermes.core.annotation.MethodDescription
import io.bluebank.hermes.core.annotation.ServiceDescription
import io.vertx.core.Future
import io.vertx.core.Future.succeededFuture


@ServiceDescription("calculator", "a simple calculator")
class CalculatorService {

  @MethodDescription(description = "add two ints")
  fun add(lhs: Int, rhs: Int): Int {
    return lhs + rhs
  }

  // N.B. how to document the return type on an async function
  @MethodDescription(description = "subtract the second int from the first", returnType = Int::class)
  fun subtract(lhs: Int, rhs: Int): Future<Int> {
    return succeededFuture(lhs - rhs)
  }
}
