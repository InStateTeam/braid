package io.bluebank.jsonrpc

data class JsonRPCResponsePayload(val result : Any?, val id: Any? = null, val jsonrpc : String = "2.0")