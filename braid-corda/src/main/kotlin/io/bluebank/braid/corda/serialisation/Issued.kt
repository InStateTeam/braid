package io.bluebank.braid.corda.serialisation

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.vertx.core.json.Json
import net.corda.core.contracts.Issued
import net.corda.core.contracts.PartyAndReference
import java.util.*

private val _PRODUCT_TYPE_FIELD = "_productType"
private val _ISSUER_FIELD = "issuer"
private val _PRODUCT_FIELD = "product"

class IssuedSerializer : StdSerializer<Issued<Any>>(Issued::class.java) {
  override fun serialize(value: Issued<Any>, generator: JsonGenerator, provider: SerializerProvider) {
    generator.writeStartObject()
    generator.writeObjectField(_ISSUER_FIELD, value.issuer)
    val product = value.product
    when (product) {
      is String -> generator.writeStringField(_PRODUCT_FIELD, product)
      is Currency -> generator.writeObjectField(_PRODUCT_FIELD, product)
      else -> {
        generator.writeObjectField(_PRODUCT_FIELD, product)
        generator.writeObjectField(_PRODUCT_TYPE_FIELD, product.javaClass.name)
      }
    }
    generator.writeEndObject()
  }
}

class IssuedDeserializer : StdDeserializer<Issued<Any>>(Issued::class.java) {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): Issued<Any> {
    val node = parseNode(parser)
    checkNode(node, parser)
    val partyAndRef = parserPartyAndRef(node)
    val product = parseProduct(node)
    return Issued(partyAndRef, product)
  }

  private fun parseNode(parser: JsonParser): JsonNode {
    return parser.readValueAsTree()
  }

  private fun checkNode(node: JsonNode, parser: JsonParser) {
    checkIsObject(node, parser)
    checkHasField(_ISSUER_FIELD, node, parser)
    checkHasField(_PRODUCT_FIELD, node, parser)
  }

  private fun parseProduct(node: JsonNode): Any {
    return if (node.has(_PRODUCT_TYPE_FIELD)) {
      parserProductByType(node)
    } else {
      parseProductFromString(node)
    }
  }

  private fun parseProductFromString(node: JsonNode): Any {
    val productString = node[_PRODUCT_FIELD].textValue()
    return try {
      Currency.getInstance(productString)
    } catch (err: IllegalArgumentException) {
      productString
    }
  }

  private fun parserProductByType(node: JsonNode): Any {
    val productClass = Class.forName(node[_PRODUCT_TYPE_FIELD].textValue())
    return Json.mapper.readerFor(productClass).readValue<Any>(node[_PRODUCT_FIELD])
  }

  private fun parserPartyAndRef(node: JsonNode): PartyAndReference {
    return Json.mapper.readerFor(PartyAndReference::class.java).readValue<PartyAndReference>(node[_ISSUER_FIELD])
  }
}