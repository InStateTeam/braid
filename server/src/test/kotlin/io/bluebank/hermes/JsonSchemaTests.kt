package io.bluebank.hermes

import io.bluebank.hermes.server.describeClass
import org.junit.Test
import kotlin.test.assertEquals

class TestJacksonSchemas {
  @Test
  fun test() {
    assertEquals("integer", describeClass(Int::class.java))
    assertEquals("string", describeClass(String::class.java))
    assertEquals("{name:string,address:{houseNumber:string,postcode:string}}",
        describeClass(Customer::class.java))
  }
}

class Customer(val name: String, val address: Address)
class Address(val houseNumber: String, val postcode: String)