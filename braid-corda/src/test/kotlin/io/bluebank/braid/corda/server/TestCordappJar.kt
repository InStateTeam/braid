package io.bluebank.braid.corda.server

import net.corda.testing.node.TestCordapp
import net.corda.testing.node.internal.TestCordappInternal
import java.nio.file.Path

/**
 * An adapter from an existing jar file to
 */
class TestCordappJar(override val config: Map<String, Any> = emptyMap(), override val jarFile: Path) :
  TestCordappInternal() {

  override fun withOnlyJarContents(): TestCordappInternal {
    return TestCordappJar(emptyMap(), jarFile)
  }

  override fun withConfig(config: Map<String, Any>): TestCordapp {
    return TestCordappJar(config, jarFile)
  }
}