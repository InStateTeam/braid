package io.bluebank.braid.corda.integration

import io.bluebank.braid.core.logging.loggerFor
import io.bluebank.braid.core.meta.DEFAULT_API_MOUNT
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavascriptIntegrationTests {
  companion object {
    private val log = loggerFor<JavascriptIntegrationTests>()
  }
  private val cordaNet = CordaNet()

  @Test
  fun runNPMTests() {
    cordaNet.withCluster {
      log.info("project directory is ${getProjectDirectory()}")
      val testDir = getProjectDirectory().resolve("../braid-client-js")
      assertTrue { testDir.exists() }
      val pb = ProcessBuilder("npm", "run", "test:integration")
      pb.environment().put("braidService", "https://localhost:${cordaNet.braidStartingPort}$DEFAULT_API_MOUNT")
      pb.directory(testDir)
      val process = pb.start()
      Thread {
        try {
          BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
            reader.lines().forEach { log.error(it) }
          }
        } catch (e: Exception) {
        }
      }.start()
      Thread {
        try {
          BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            reader.lines().forEach { log.info(it) }
          }
        } catch (e: Exception) {
        }
      }.start()
      process.waitFor(20, TimeUnit.SECONDS)
      assertEquals(0, process.exitValue(), "tests should succeed")
    }
  }

  private fun getProjectDirectory() = File(System.getProperty("user.dir"))
}