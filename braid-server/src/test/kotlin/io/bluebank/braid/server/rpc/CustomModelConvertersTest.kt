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
package io.bluebank.braid.server.rpc


import io.bluebank.braid.corda.serialisation.AmountSerializer
import io.bluebank.braid.corda.serialisation.BraidCordaJacksonInit
import io.bluebank.braid.corda.swagger.CustomModelConverters
import io.swagger.converter.ModelConverters
import io.vertx.core.json.Json
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.transactions.CoreTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.GBP
import net.corda.testing.core.DUMMY_BANK_A_NAME
import net.corda.testing.core.TestIdentity
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.Arrays.asList
import kotlin.test.assertEquals


class CustomModelConvertersTest{
    companion object {
        val DUMMY_BANK_A = TestIdentity(DUMMY_BANK_A_NAME, 40).party
    }
    @Before fun setUp(){
        BraidCordaJacksonInit.init()
        CustomModelConverters.init()

    }

    @Test
    fun `should Correctly Model Amount`() {
        val models = ModelConverters.getInstance().readAll(ClassWithTypes::class.java)
        println(models)

        assertThat(models.toString(),models.get("Amount"), notNullValue())
        val properties = models.get("Amount")?.properties

        assertThat(properties?.toString(), properties?.size, equalTo(4))
        assertThat(properties?.toString(), properties?.get("quantity")?.type, equalTo("integer"))
        assertThat(properties?.toString(), properties?.get("displayTokenSize")?.type, equalTo("number"))
        assertThat(properties?.toString(), properties?.get("token")?.type, equalTo("string"))
        assertThat(properties?.toString(), properties?.get("_tokenType")?.type, equalTo("string"))

    }


    @Test
    fun `should Correctly Model AmountCurrency and AmountString`() {
        val models = ModelConverters.getInstance().readAll(ClassWithTypes::class.java)
        println(models)

        val model = models.get("AmountCurrency")
        assertThat(models.toString(), model, notNullValue())
        val properties = model?.properties

        assertThat(properties?.toString(), properties?.size, equalTo(3))
        assertThat(properties?.toString(), properties?.get("quantity")?.type, equalTo("integer"))
        assertThat(properties?.toString(), properties?.get("displayTokenSize")?.type, equalTo("number"))
        assertThat(properties?.toString(), properties?.get("token")?.type, equalTo("string"))
    }

    @Test
    fun `should Correctly Model OpaqueBytes as string`() {
        val models = ModelConverters.getInstance().readAll(ClassWithTypes::class.java)
        println(models)

        val model = models.get("ClassWithTypes")
        assertThat(models.toString(), model, notNullValue())

        val properties = model?.properties
        assertThat(properties?.keys, hasItem("bytes"))
        assertThat(properties?.toString(), properties?.get("bytes")?.type, equalTo("string"))
    }
    
    @Test
    fun `should Correctly Model SecureHash as string`() {
        val models = ModelConverters.getInstance().readAll(ClassWithTypes::class.java)
        println(models)

        val model = models.get("ClassWithTypes")
        assertThat(models.toString(), model, notNullValue())

        val properties = model?.properties
        assertThat(properties?.keys, hasItem("hash"))
        assertThat(properties?.toString(), properties?.get("hash")?.type, equalTo("string"))
    }

  @Test
    fun `should Correctly Model Issuer as string`() {
        val models = ModelConverters.getInstance().readAll(ClassWithTypes::class.java)
        println(models)

        val model = models.get("ClassWithTypes")
        assertThat(models.toString(), model, notNullValue())

        val properties = model?.properties
        assertThat(properties?.keys, hasItem("hash"))
        assertThat(properties?.toString(), properties?.get("hash")?.type, equalTo("string"))
    }




    @Test
    fun `that OpaqueBytes can be serialised and deserialised`() {
        val expected = OpaqueBytes( "someBytes".toByteArray())
        val encoded = Json.encode(expected)

        assertEquals(encoded, "\"736F6D654279746573\"")
    }

    @Test
    fun `that Amount of String token can be serialised and deserialised`() {
        val expected = Amount(100, "GBP")
        val encoded = Json.encode(expected)

        assertEquals(encoded, "{\"quantity\":100,\"displayTokenSize\":1,\"token\":\"GBP\"}")
    }

    @Test
    fun `that Amount of Currency token can be serialised and deserialised`() {
        val expected = Amount(100, GBP)
        val encoded = Json.encode(expected)
        assertEquals(encoded, "{\"quantity\":100,\"displayTokenSize\":0.01,\"token\":\"GBP\"}")
    }

//   @Test
//    fun `that SignedTransaction can be serialised and deserialised`() {
//        val expected = SignedTransaction(CoreTransaction(),asList())
//        val encoded = Json.encode(expected)
//        assertEquals(encoded, "{\"quantity\":100,\"displayTokenSize\":0.01,\"token\":\"GBP\"}")
//    }



    @Test
    fun `that Amount of Issued Currency can be serialised and deserialised`() {
        val expected =
                Amount(100, Issued(PartyAndReference(DUMMY_BANK_A, OpaqueBytes.of(0x01)), GBP))
        val encoded = Json.encode(expected)
        assertEquals(encoded, "{\"quantity\":100," +
                "\"displayTokenSize\":0.01," +
                "\"token\":{\"issuer\":{\"party\":{\"name\":\"O=Bank A, L=London, C=GB\",\"owningKey\":\"GfHq2tTVk9z4eXgyUuofmR16H6j7srXt8BCyidKdrZL5JEwFqHgDSuiinbTE\"},\"reference\":\"01\"},\"product\":\"GBP\"}," +
                "\"_tokenType\":\"net.corda.core.contracts.Issued\"}")
    }






    @Test
    fun `should Correctly Model Party as owning key string`() {

        val models = ModelConverters.getInstance().readAll(ClassWithTypes::class.java)
        println(models)

        val properties = models.get("Party")?.properties
        assertThat(properties?.size, equalTo(2))
        assertThat(properties?.keys, hasItem("name"))
        assertThat(properties?.toString(), properties?.get("name")?.type, equalTo("string"))

        assertThat(properties?.keys, hasItem("owningKey"))
        assertThat(properties?.toString(), properties?.get("owningKey")?.type, equalTo("string"))
    }

    data class ClassWithTypes(
             val amountCurrency:Amount<Currency>
            ,val amountString:Amount<String>
            ,val amount:Amount<Any>
            ,val party:Party
            ,val bytes:OpaqueBytes
            ,val hash:SecureHash
            ,val isduedString:Issued<String>
            ,val isduedCurrency:Issued<Currency>
            ,val issued:Issued<Currency>
    )

}