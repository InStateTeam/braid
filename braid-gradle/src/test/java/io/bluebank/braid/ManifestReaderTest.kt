package io.bluebank.braid

import org.hamcrest.CoreMatchers
import org.junit.Test

import org.junit.Assert.*

class ManifestReaderTest {

  @Test
  fun getManifestUrl() {
    val reader = ManifestReader("https://repo1.maven.org/maven2/io/bluebank/braid/braid-server/maven-metadata.xml")

    assertThat(reader.latest(), CoreMatchers.`is`("4.1.2-RC08"))
  }
}