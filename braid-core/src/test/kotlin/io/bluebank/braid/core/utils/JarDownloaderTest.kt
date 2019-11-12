package io.bluebank.braid.core.utils

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Test

import org.junit.Assert.*
import java.io.File
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import java.net.MalformedURLException
import java.net.URL

class JarDownloaderTest {

  private val jarDownloader = JarDownloader()

  @Test
  fun `should download if snapshot and we dont have`() {
    val url = URL("http://repo1.maven.org/maven2/net/corda/corda-finance-workflows/4.0-SNAPSHOT/corda-finance-workflows-4.0-SNAPSHOT.jar")
    assertThat(jarDownloader.shouldDownload(url, false), equalTo(true))
  }

 @Test
  fun `should download if snapshot and we do have`() {
   val url = URL("http://repo1.maven.org/maven2/net/corda/corda-finance-workflows/4.0-SNAPSHOT/corda-finance-workflows-4.0-SNAPSHOT.jar")
    assertThat(jarDownloader.shouldDownload(url, false), equalTo(true))
  }

  @Test
  fun `should not download if versioned jar and we have it`() {
    val url = URL("http://repo1.maven.org/maven2/net/corda/corda-finance-workflows/4.0/corda-finance-workflows-4.0.jar")
    assertThat(jarDownloader.shouldDownload(url,true), equalTo(false))
  }

  @Test
  fun `should download if versioned jar and we dont have it`() {
    val url = URL("http://repo1.maven.org/maven2/net/corda/corda-finance-workflows/4.0/corda-finance-workflows-4.0.jar")
    assertThat(jarDownloader.shouldDownload(url,false), equalTo(true))
  }

}