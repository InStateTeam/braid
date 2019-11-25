package io.bluebank.braid

import org.hamcrest.CoreMatchers
import org.junit.Assert.*
import org.junit.Test
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import org.junit.Ignore

class HoconReaderTest{

  @Test
  @Ignore
  fun `we just want to read the config file not load it`() {
    val config = HoconReader().read("/node.conf")

    assertThat(config.getString(""), CoreMatchers.`is`(""))
  }
}