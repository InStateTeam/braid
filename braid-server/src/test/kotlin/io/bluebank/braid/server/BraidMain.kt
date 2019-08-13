package io.bluebank.braid.server

import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.Future

private val log = loggerFor<Braid>()

fun main(args: Array<String>) {

    if (args.size != 4) {
        throw IllegalArgumentException("Usage: Braid <node address> <username> <password> <port>")
    }

    val port = Integer.valueOf(args[3])
    Braid()
            .withNodeAddress(args[0])
            .withUserName(args[1])
            .withPassword(args[2])
            .withPort(port)
            .startServer()
            .recover {
                log.error("Server failed to start:", it)
                Future.succeededFuture("-1")
            }
            .apply {
                log.info("Braid started on port:$port")
                ProcessBuilder().command("open" , "http://localhost:$port/swagger.json")   .start()
                ProcessBuilder().command("open" , "http://localhost:$port/api/rest/cordapps/flows")   .start()
            }




    //connection.notifyServerAndClose()
}
