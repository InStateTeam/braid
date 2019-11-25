package io.bluebank.braid

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.io.File

class HoconReader{
  fun read(file: String): Config {
    return ConfigFactory.systemProperties()
        .withFallback(ConfigFactory.parseResources(file))
        .withFallback(ConfigFactory.parseFile(File(file)))
  }

}