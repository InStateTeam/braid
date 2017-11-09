package io.bluebank.hermes.corda.serialisation

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.JsonNode


internal fun checkIsObject(node: JsonNode, parser: JsonParser) {
  if (!node.isObject) {
    throw JsonMappingException.from(parser, "must be an object")
  }
}

internal fun checkHasField(fieldName: String, node: JsonNode, parser: JsonParser) {
  if (!node.has(fieldName)) {
    throw JsonMappingException.from(parser, "missing field: $fieldName")
  }
}
