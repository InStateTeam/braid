package io.bluebank.braid.server

import net.corda.core.utilities.NetworkHostAndPort
import org.testcontainers.containers.GenericContainer
import kotlin.test.Test

class DockerTest {
  @Test
  fun `that we can start a simple network`() {
    val networkHostAndPort = NetworkHostAndPort.parse("o-partya-l-london-c-gb:60156")
  }
}

class KGenericContainer(imageName: String) : GenericContainer<KGenericContainer>(imageName)

fun KGenericContainer.use(fn: (KGenericContainer) -> Unit) {
  fn(this)
  stop()
}
