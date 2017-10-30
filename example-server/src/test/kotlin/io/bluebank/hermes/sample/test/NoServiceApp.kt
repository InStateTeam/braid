package io.bluebank.hermes.sample.test

import io.bluebank.hermes.server.JsonRPCServerBuilder

fun main(args: Array<String>) {
  JsonRPCServerBuilder.createServerBuilder()
      .build()
      .start()
}
