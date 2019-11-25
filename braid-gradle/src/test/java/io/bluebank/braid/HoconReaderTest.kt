package io.bluebank.braid

import org.hamcrest.CoreMatchers
import org.junit.Assert.*
import org.junit.Test
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Ignore

class HoconReaderTest{
  companion object{
    val config = HoconReader().read("web-server.conf")
  }


  @Test
  fun `read boolean`() {
    assertThat(config.getBoolean("devMode"), equalTo(true))
  }

  @Test
  fun `read config`() {
    assertThat(config.getConfig("rpcSettings"), notNullValue())
  }

  @Test
  fun `read rps address`() {
    assertThat(config.getString("rpcSettings.address"), equalTo("localhost:10006"))
  }

  @Test
  fun `read user in array`() {
    assertThat(config.getConfigList("security.authService.dataSource.users").get(0).getString("user"), equalTo("user1"))
  }

  @Test
  fun `read password in array`() {
    assertThat(config.getConfigList("security.authService.dataSource.users").get(0).getString("password"), equalTo("test"))
  }

  @Test
  fun `read port`() {
    assertThat(config.getString("webAddress"), equalTo("localhost:10007"))
  }

  @Test
  fun `ishould read file`() {
    val read = HoconReader().read(HoconReaderTest::class.java.getResource("/web-server.conf").file)

    assertThat(read.hasPath("webAddress"), equalTo(true))
 }

  @Test
  fun `identify missing properties`() {
    val read = HoconReader().read("missing.conf")

    assertThat(config.hasPath("webAddress"), equalTo(true))
    assertThat(read.hasPath("webAddress"), equalTo(false))
  }
}