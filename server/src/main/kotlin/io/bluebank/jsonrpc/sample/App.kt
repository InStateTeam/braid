package io.bluebank.jsonrpc.sample

import io.bluebank.jsonrpc.server.JsonRPCServer


fun main(args: Array<String>) {
  JsonRPCServer("/api/", 8080, listOf(CalculatorService())).start()
}