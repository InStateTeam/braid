package io.bluebank.braid

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class HoconReader{
  fun read(resource: String): Config {
    return ConfigFactory.load(resource)
  }

}