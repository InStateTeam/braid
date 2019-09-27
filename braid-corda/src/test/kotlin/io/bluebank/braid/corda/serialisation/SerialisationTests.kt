/**
 * Copyright 2018 Royal Bank of Scotland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.bluebank.braid.corda.serialisation

import io.bluebank.braid.corda.server.BraidCordaStandaloneServer
import io.vertx.core.json.Json
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference
import net.corda.core.utilities.NonEmptySet
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.GBP
import net.corda.testing.core.DUMMY_BANK_A_NAME
import net.corda.testing.core.SerializationEnvironmentRule
import net.corda.testing.core.TestIdentity
import org.hamcrest.CoreMatchers.startsWith
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import sun.security.provider.X509Factory
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.jvm.javaType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SerialisationTests {
  companion object {
    val DUMMY_BANK_A = TestIdentity(DUMMY_BANK_A_NAME, 40).party
  }

  @Rule
  @JvmField
  val testSerialization = SerializationEnvironmentRule()

  @Before
  fun before() {
    BraidCordaStandaloneServer.init()
  }

//  @Ignore
  @Test     // fails because we cant tell if this is a String or a Currency
  fun `that Amount of String token can be serialised and deserialised`() {
    val expected = Amount(100, "GBP")
    val encoded = Json.encode(expected)
    val actual = Json.decodeValue(encoded, Amount::class.java)
    assertEquals(expected, actual)
  }

  @Test
  fun `that Amount of Currency token can be serialised and deserialised`() {
    val expected = Amount(100, GBP)
    val encoded = Json.encode(expected)
    val actual = Json.decodeValue(encoded, Amount::class.java)
    assertEquals(expected, actual)
  }

  @Test
  fun `that Amount of Issued Currency can be serialised and deserialised`() {
    val expected =
      Amount(100, Issued(PartyAndReference(DUMMY_BANK_A, OpaqueBytes.of(0x01)), GBP))
    val encoded = Json.encode(expected)
    val actual = Json.decodeValue(encoded, Amount::class.java)
    assertEquals(expected, actual)
  }

  @Test
  fun `that Date should serialized using ISO8601`() {
    val expected = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").parse("2019-03-30 12:34:56.567")
    val encoded = Json.encode(expected)
    
    assertEquals("\"2019-03-30T12:34:56.567+0000\"", encoded)
  }

  @Test
  fun `that X509 Should serialize as bytes`() {
    val base64 = this::class.java.getResource("/serlialization/certificate/x509.pem")
        .readText()
        .replace(X509Factory.BEGIN_CERT,"")
        .replace(X509Factory.END_CERT,"")
        .replace("\n","")
        .replace("\r","")

    val certificate = CertificateFactory.getInstance("X.509")
        .generateCertificate(ByteArrayInputStream(Base64.getDecoder().decode(base64)))

    val encoded = Json.encode(certificate)
    val decoded = Json.decodeValue(encoded, X509Certificate::class.java)

    assertThat(encoded, startsWith("\"MIIIRzCCBi"))
  }

  @Test
  fun `given a non empty set we can deserialise it`() {
    val json = """
      [
        "item1", 
        "item2"
      ]
    """.trimIndent()
    val type = ::f.parameters.first().type.javaType
    val javaType = Json.mapper.typeFactory.constructType(type)
    val result = Json.mapper.readValue<NonEmptySet<String>>(json, javaType)
    assertTrue(result is NonEmptySet)
    assertEquals(2, result.size)
    assertTrue(result.contains("item1") && result.contains("item2"))
  }

  private fun f(set: NonEmptySet<String>) {}
}
