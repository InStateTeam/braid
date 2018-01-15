package io.bluebank.braid.integration

import io.bluebank.braid.core.socket.findFreePort
import io.bluebank.braid.integration.server.TestServer
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(VertxUnitRunner::class)
class JavascriptIntegrationTests {
  private lateinit var server: TestServer
  private val port = findFreePort()
  @Before
  fun before(context: TestContext) {
    server = TestServer(port)
    server.start(context.asyncAssertSuccess<Void>()::handle)
  }

  @After
  fun after() {
    server.stop()
  }

  @Test
  fun runNPMTests() {
    val testDir = getProjectDirectory().resolve("../braid-client-js")
    assertTrue { testDir.exists() }
    val pb = ProcessBuilder("npm", "run", "test:integration")
    pb.environment().put("braidService", "https://localhost:$port/api/myservice/braid")
    pb.directory(testDir)
    val process = pb.start()
    Thread {
      try {
        BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
          reader.lines().forEach { println(it) }
        }
      } catch (e: Exception) {
      }
    }.start()
    Thread {
      try {
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
          reader.lines().forEach { println(it) }
        }
      } catch (e: Exception) {
      }
    }.start()
    process.waitFor(20, TimeUnit.SECONDS)
    assertEquals(0, process.exitValue(), "tests should succeed")
  }

  private fun getProjectDirectory() = File(System.getProperty("user.dir"))
}