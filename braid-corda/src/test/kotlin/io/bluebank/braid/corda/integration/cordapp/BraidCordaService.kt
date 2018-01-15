package io.bluebank.braid.corda.integration.cordapp

import io.bluebank.braid.corda.BraidConfig
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
class BraidCordaService(private val serviceHub: ServiceHub) : SingletonSerializeAsToken() {
  init {
    BraidConfig()
        .withFlow("echo", EchoFlow::class)
        .withService("myService", CustomService(serviceHub))
        .withAuthConstructor(this::shiroFactory)
        .withPort(getBraidPort())
        .bootstrapBraid(serviceHub)
  }

  private fun shiroFactory(it: Vertx): AuthProvider {
    val shiroConfig = json {
      obj {
        put("properties_path", "classpath:shiro.properties")
      }
    }
    return ShiroAuth.create(it, ShiroAuthOptions().setConfig(shiroConfig))
  }

  private fun getBraidPort() : Int {
    val org = serviceHub.myInfo.legalIdentities.first().name.organisation
    val property = "braid.$org.port"
    return when {
      System.getProperties().containsKey(property) -> System.getProperty(property).toInt()
      else -> 8080
    }
  }
}