package io.bluebank.braid.core.jsonrpc

open class JsonRPCResponse
data class JsonRPCResultResponse(val result : Any?, val id: Any? = null, val jsonrpc : String = "2.0") : JsonRPCResponse()
data class JsonRPCCompletedResponse(val id: Any? = null, val jsonrpc: String = "2.0", val completed: Boolean = true) : JsonRPCResponse()