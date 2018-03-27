/*
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

import io.vertx.core.json.Json
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.GBP
import net.corda.testing.core.DUMMY_BANK_A_NAME
import net.corda.testing.core.SerializationEnvironmentRule
import net.corda.testing.core.TestIdentity
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class SerialisationTests {
  companion object {
    val DUMMY_BANK_A = TestIdentity(DUMMY_BANK_A_NAME, 40).party
  }

  @Rule
  @JvmField
  val testSerialization = SerializationEnvironmentRule()

  @Before
  fun before() {
    BraidCordaJacksonInit.init()
  }

  @Ignore
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
    val expected = Amount(100, Issued(PartyAndReference(DUMMY_BANK_A, OpaqueBytes.of(0x01)), GBP))
    val encoded = Json.encode(expected)
    val actual = Json.decodeValue(encoded, Amount::class.java)
    assertEquals(expected, actual)
  }
}
