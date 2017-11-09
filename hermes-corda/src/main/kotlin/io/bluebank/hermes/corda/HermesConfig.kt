package io.bluebank.hermes.corda

import io.vertx.core.Vertx
import io.vertx.ext.auth.AuthProvider
import net.corda.core.flows.FlowLogic

data class HermesConfig(val port : Int = 8080,
                   val rootPath: String = "/api/jsonrpc/",
                   val registeredFlows : Map<String, Class<out FlowLogic<*>>> = emptyMap(),
                   val authConstructor: ((Vertx) -> AuthProvider)? = null) {

  fun <T: FlowLogic<*>> withFlow(flowClass: Class<T>) : HermesConfig {
    return withFlow(flowClass.simpleName.decapitalize(), flowClass)
  }

  fun <T : FlowLogic<*>> withFlow(name : String, flowClass: Class<T>) : HermesConfig {
    val map = registeredFlows.toMutableMap()
    map.put(name, flowClass)
    return this.copy(registeredFlows = map)
  }
}