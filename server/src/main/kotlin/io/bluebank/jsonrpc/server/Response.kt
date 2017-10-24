package io.bluebank.jsonrpc.server

open class JsonRPCResponse
data class JsonRPCResultResponse(val result : Any?, val id: Any? = null, val jsonrpc : String = "2.0") : JsonRPCResponse()