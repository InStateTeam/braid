package io.bluebank.braid.corda.integration.cordapp

import io.bluebank.braid.corda.BraidConfig
import io.bluebank.braid.core.logging.loggerFor
import io.vertx.core.Vertx
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
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
          .withAuthConstructor(this::shiroFactory)
          .withPort(port)
          .bootstrapBraid(serviceHub)
    } else {
      log.info("No port defined for $org")
    }
  }

  private fun shiroFactory(it: Vertx): AuthProvider {
    val shiroConfig = json {
      obj {
        put("properties_path", "classpath:auth/shiro.properties")
      }
    }
    return ShiroAuth.create(it, ShiroAuthOptions().setConfig(shiroConfig))
  }

  private fun getBraidPort() : Int {
    val property = "braid.$org.port"
    return System.getProperty(property)?.toInt() ?: when(org) {
      "PartyA" -> 8080
      "PartyB" -> 8081
      else -> 0
    }
  }
}