package io.bluebank.braid.sample.test

import io.bluebank.braid.server.JsonRPCServerBuilder

fun main(args: Array<String>) {
  JsonRPCServerBuilder.createServerBuilder()
      .build()
      .start()
}
