package io.bluebank.jsonrpc.server

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class JsonRPCService(val name: String, val description: String)
