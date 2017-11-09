package io.bluebank.hermes.corda.serialisation

import io.vertx.core.json.Json
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.GBP
import net.corda.testing.DUMMY_BANK_A
import net.corda.testing.withTestSerialization
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SerialisationTests {
  @Before
  fun before() {
    HermesCordaJacksonInit.init()
  }

  @Test
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
    withTestSerialization {
      val expected = Amount(100, Issued(PartyAndReference(DUMMY_BANK_A, OpaqueBytes.of(0x01)), GBP))
      val encoded = Json.encode(expected)
      val actual = Json.decodeValue(encoded, Amount::class.java)
      assertEquals(expected, actual)
    }
  }
}
