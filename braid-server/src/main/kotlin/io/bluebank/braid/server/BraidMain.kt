package io.bluebank.braid.server

import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.utils.tryWithClassLoader
import io.bluebank.braid.server.util.toCordappsClassLoader
import io.vertx.core.Future
import net.corda.core.utilities.NetworkHostAndPort

private val log = loggerFor<BraidMain>()

fun main(args: Array<String>) {
  if (args.size < 4) {
    throw IllegalArgumentException("Usage: BraidMainKt <node address> <username> <password> <port> [<cordaAppJar1> <cordAppJar2> ....]")
  }

  val networkAndPort = args[0]
  val userName = args[1]
  val password = args[2]
  val port = Integer.valueOf(args[3])
  val additionalPaths = args.asList().drop(4)
  BraidMain().start(networkAndPort, userName, password, port, additionalPaths)
}

class BraidMain {

  fun start(networkAndPort: String,userName: String, password: String, port: Int, additionalPaths: List<String>): Future<String> {
    val classLoader = additionalPaths.toCordappsClassLoader()
    return tryWithClassLoader(classLoader) {
      Braid(
          port = port,
          userName = userName,
          password = password,
          nodeAddress = NetworkHostAndPort.parse(networkAndPort)
      )
          .startServer()
          .recover {
            log.error("Server failed to start:", it)
            Future.succeededFuture("-1")
          }
    }
  }
}