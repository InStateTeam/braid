package io.bluebank.braid.server.util

import org.junit.Ignore
import org.junit.Test
import java.net.URL
import kotlin.test.assertEquals

class URIUtilsTest {
  companion object {
    const val CORDAPP_NAME = "my-cordapp"
    const val CORDAPP_VERSION = "1.1"
    const val CORDAPP_JAR = "$CORDAPP_NAME-$CORDAPP_VERSION.jar"
  }

  @Ignore
  @Test
  fun `given a valid cordapp path that we can extract the cordapp name`() {
    val cordappName = URL("file:///foo/bar/$CORDAPP_JAR").getCordappName()
    assertEquals(CORDAPP_NAME, cordappName)
  }
}