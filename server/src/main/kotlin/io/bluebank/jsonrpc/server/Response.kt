package io.bluebank.jsonrpc.server

data class JsonRPCResponsePayload(val result : Any?, val id: Any? = null, val jsonrpc : String = "2.0")