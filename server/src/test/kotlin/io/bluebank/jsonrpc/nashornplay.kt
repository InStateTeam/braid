package io.bluebank.jsonrpc

import io.vertx.core.AsyncResult
import java.util.logging.Handler
import javax.script.Invocable
import javax.script.ScriptEngineManager


fun main(args: Array<String>) {
  val sem = ScriptEngineManager()
  val engine = sem.getEngineByName("nashorn")
  val script = """
    var console = { log : print }
    function foo(lhs, rhs, callback) {
      console.log(callback);
      callback.invoke(lhs + rhs);
    }

    """
  engine.eval(script)
  
  val r = engine.eval("(typeof blah) === 'function'")
  val invocable = engine as Invocable
  val result = invocable.invokeFunction("foo", 1, 2, { result : Double ->
    println(result)
  })

}