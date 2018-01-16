package io.bluebank.braid.corda.integration.cordapp

import io.bluebank.braid.corda.BraidConfig
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.AsyncResult
import io.vertx.core.Future.failedFuture
import io.vertx.core.Future.succeededFuture
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AbstractUser
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken

@CordaService
class TestBraidCordaService(private val serviceHub: ServiceHub) : SingletonSerializeAsToken() {
  companion object {
    private val log = loggerFor<TestBraidCordaService>()
  }

  private val org = serviceHub.myInfo.legalIdentities.first().name.organisation

  init {
    val port = getBraidPort()
    if (port > 0) {
      log.info("Starting Braid service for $org on port $port")
      BraidConfig()
          .withFlow("echo", EchoFlow::class)
          .withService("myService", CustomService(serviceHub))
          .withAuthConstructor({_ -> MyAuthService()})
          .withPort(port)
          .bootstrapBraid(serviceHub)
    } else {
      log.info("No port defined for $org")
    }
  }

  private fun getBraidPort() : Int {
    val property = "braid.$org.port"
    return System.getProperty(property)?.toInt() ?: when(org) {
      "PartyA" -> 8080
      "PartyB" -> 8081
      else -> 0
    }
  }

  class MyAuthService : AuthProvider {
    override fun authenticate(authInfo: JsonObject, resultHandler: Handler<AsyncResult<User>>) {
      val username = authInfo.getString("username", "")
      val password = authInfo.getString("password", "")
      if (username == "admin" && password == "admin") {
        resultHandler.handle(succeededFuture(MyAuthUser(username)))
      } else {
        resultHandler.handle(failedFuture("authentication failed"))
      }
    }
  }

  class MyAuthUser(username: String): AbstractUser() {
    private val principal = JsonObject().put("username", username)

    override fun doIsPermitted(permission: String, resultHandler: Handler<AsyncResult<Boolean>>) {
      // all is permitted
      resultHandler.handle(succeededFuture(true))
    }

    override fun setAuthProvider(authProvider: AuthProvider?) {
    }

    override fun principal(): JsonObject {
      return principal
    }
  }
}