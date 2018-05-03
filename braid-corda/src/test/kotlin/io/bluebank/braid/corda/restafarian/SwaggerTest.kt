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

package io.bluebank.braid.corda.restafarian

import io.netty.handler.codec.http.HttpHeaderValues
import io.swagger.annotations.ApiModelProperty
import io.swagger.converter.ModelConverters
import io.swagger.models.*
import io.swagger.models.auth.ApiKeyAuthDefinition
import io.swagger.models.auth.In
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.properties.PropertyBuilder
import io.swagger.models.properties.RefProperty
import io.swagger.util.Yaml
import org.junit.Test
import java.lang.reflect.Type
import java.math.BigDecimal
import java.util.*
import kotlin.reflect.KClass

data class Tag(val category: String, val value: String)

data class CreateAccountRequest(
    @ApiModelProperty(example = "my-account-1")
    val accountId: String,
    @ApiModelProperty(example = "GBP")
    val currency: Currency,
    val functionalUnitId: UUID,
    val aliases: Set<Tag> = emptySet(),
    @ApiModelProperty(example = "0")
    val minimumBalance : BigDecimal = BigDecimal.ZERO)

data class AccountAddress(val accountId: String, val functionalUnit: UUID, val organisation: String) {
  companion object {
    private val RE = Regex("^([^:]+):([^:]+):([^:]+)$")
    private val ACCOUNT_ID_RE = Regex("^[^:\\s]+$")
    fun parse(address: String): AccountAddress {
      val mr = RE.matchEntire(address) ?: throw ParseException("failed to parse $address")
      val (_, accountId, functionalUnitStr, party) = mr.groupValues
      val functionalUnitId = UUID.fromString(functionalUnitStr)
      return AccountAddress(accountId, functionalUnitId, party)
    }

    fun create(accountId: String, organisation: String, functionalUnitId: UUID): AccountAddress {
      validateAccountId(accountId)
      return AccountAddress(accountId, functionalUnitId, organisation)
    }

    private fun validateAccountId(accountId: String) {
      if (!ACCOUNT_ID_RE.matches(accountId)) {
        throw ParseException("accountId must match ${ACCOUNT_ID_RE.pattern}")
      }
    }
  }

  override fun toString(): String {
    return "$accountId:$functionalUnit:$organisation"
  }

  class ParseException(msg: String) : RuntimeException(msg)
}

interface Account {
  val address: AccountAddress
  val currency: Currency
  val minimumBalance: BigDecimal
  val aliases: Set<Tag>
  fun getAllAliases() : Set<Tag>
}


class SwaggerTest {

  @Test
  fun swaggerBuildTest() {
    val models = mutableMapOf<String, Model>()
        .readType(CreateAccountRequest::class)
        .readType(Account::class)
    val info = Info()
        .version("1.0.0")
        .title("Swagger Petstore")

    info.contact = Contact()
        .name("Em Tech")
        .email("support@bluebank.io")
        .url("http://bluebank.io")

    val swagger = Swagger()
        .info(info)
        .host("localhost:8080")
        .securityDefinition("api-key", ApiKeyAuthDefinition("key", In.HEADER))
        .scheme(Scheme.HTTP)
        .consumes(HttpHeaderValues.APPLICATION_JSON.toString())
        .produces(HttpHeaderValues.APPLICATION_JSON.toString())
        .addAllModels(models)

    val bodyParam = BodyParameter()
        .schema(RefModel(CreateAccountRequest::class.simpleName))
        .name(CreateAccountRequest::class.simpleName)
    bodyParam.required = true

    swagger.path("/api/accounts", Path().post(
        Operation()
            .produces(HttpHeaderValues.APPLICATION_JSON.toString())
            .consumes(HttpHeaderValues.APPLICATION_JSON.toString())
            .parameter(bodyParam)
            .defaultResponse(Response()
                .description("response")
                .schema(RefProperty(Account::class.simpleName)))
    ))

    Yaml.pretty().writeValueAsString(swagger)
  }

  private inline fun <reified T : Any> MutableMap<String, Model>.readType(type: KClass<T>): MutableMap<String, Model> {
    return this.readType(type.java)
  }

  private fun MutableMap<String, Model>.readType(type: Type): MutableMap<String, Model> {
    ModelConverters.getInstance().readAll(type).forEach { k, v ->
      this[k] = v
    }
    return this
  }

  private fun Swagger.addAllModels(types: Map<String, Model>): Swagger {
    types.forEach { name, model ->
      this.model(name, model)
    }
    return this
  }

  @Test
  fun deser() {
    val yml = """---
swagger: "2.0"
info:
  version: "1.0.0"
  title: "Swagger Petstore"
  contact:
    name: "Em Tech"
    url: "http://bluebank.io"
    email: "support@bluebank.io"
host: "localhost:8080"
schemes:
- "http"
consumes:
- "application/json"
produces:
- "application/json"
paths:
  /api/accounts:
    post:
      consumes:
      - "application/json"
      produces:
      - "application/json"
      parameters:
      - in: "body"
        name: "CreateAccountRequest"
        required: true
        schema:
          type: string
      responses:
        default:
          description: "response"
          schema:
            ${'$'}ref: "#/definitions/Account"
securityDefinitions:
  api-key:
    type: "apiKey"
    name: "key"
    in: "header"
definitions:
  CreateAccountRequest:
    type: "object"
    properties:
      accountId:
        type: "string"
      currency:
        type: "string"
      id:
        type: "string"
  Account:
    type: "object"
    properties:
      address:
        ${'$'}ref: "#/definitions/AccountAddress"
      currency:
        type: "string"
      tags:
        type: "object"
        additionalProperties:
          type: "string"
      canonicalId:
        type: "string"
        readOnly: true
  AccountAddress:
    type: "object"
    properties:
      accountId:
        type: "string"
      functionalUnit:
        type: "string"
      organisation:
        type: "string"
      """

    val property = ModelConverters.getInstance().readAsProperty(Account::class.java)
    val model = PropertyBuilder.toModel(property)
    model.toString()
    Yaml.mapper().readValue(yml, Swagger::class.java)
  }
}