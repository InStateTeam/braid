package io.bluebank.jsonrpc.sample

import io.bluebank.jsonrpc.server.JsonRPCServer


fun main(args: Array<String>) {
  JsonRPCServer(rootPath = "/api/", port = 8080, services = listOf(CalculatorService(), AccountService())).start()
}