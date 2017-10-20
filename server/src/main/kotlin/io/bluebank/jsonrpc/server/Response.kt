package io.bluebank.jsonrpc.server

data class JsonRPCResponse(val result : Any?, val id: Any? = null, val jsonrpc : String = "2.0")