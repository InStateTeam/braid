package io.bluebank.braid.core.jsonrpc

import com.fasterxml.jackson.databind.type.TypeFactory
import io.bluebank.braid.core.json.BraidJacksonInit
import io.vertx.core.json.Json
import org.junit.Before
import org.junit.Test
import java.time.Instant
import kotlin.reflect.jvm.jvmErasure

data class Person(val name: String, val dob: Instant)
class TestService {
  fun getNames(people: List<Person>) : List<String>{
    return people.map { it.name }
  }
}

class ConverterTest {
  @Before
  fun before() {
    BraidJacksonInit.init()
  }

  @Test
  fun `that we can deserialise collection type`() {
    val parameter = TestService::getNames.parameters[1]
    val ktype = parameter.type
    val arguments = ktype.arguments
    val argument = arguments[0]
    val elementType = argument.type!!.jvmErasure.javaObjectType

    val people = listOf(Person("Jim", Instant.now()))
    val request = JsonRPCRequest(id = 1, method = "getNames", params = listOf(people))
    val json = Json.encode(request)
    val request2 = Json.decodeValue(json, JsonRPCRequest::class.java)
    val params = request2.params as List<*>
    val param1 = params[0]
    Json.encode(param1)
    val collectionType = TypeFactory.defaultInstance().constructCollectionType(ArrayList::class.java, elementType)
    val any = Json.mapper.convertValue<ArrayList<*>>(param1!!, collectionType)
    val anyList = any.toList()
    val testService = TestService()
    @Suppress("UNCHECKED_CAST")
    TestService::getNames.invoke(testService, anyList as List<Person>)
  }
}

