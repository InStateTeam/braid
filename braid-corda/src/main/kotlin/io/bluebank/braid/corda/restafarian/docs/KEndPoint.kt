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
package io.bluebank.braid.corda.restafarian.docs

import io.bluebank.braid.corda.restafarian.Paths
import io.netty.handler.codec.http.HttpHeaderValues
import io.swagger.annotations.ApiParam
import io.swagger.models.parameters.BodyParameter
import io.swagger.models.parameters.Parameter
import io.swagger.models.parameters.PathParameter
import io.swagger.models.parameters.QueryParameter
import io.vertx.core.http.HttpMethod
import java.lang.reflect.Type
import javax.ws.rs.DefaultValue
import javax.ws.rs.core.MediaType
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaType

class KEndPoint(groupName: String,
                protected: Boolean,
                method: HttpMethod,
                path: String,
                val name: String,
                val parameters: List<KParameter>,
                override val returnType: Type,
                override val annotations: List<Annotation>) : EndPoint(groupName, protected, method, path) {

  init {
    // TODO: check sanity of method parameters and types vs REST/HTTP limitations
  }

  private val pathParams = Paths.PATH_PARAMS_RE.findAll(path)
    .map { it.groups[2]!!.value }
    .map { paramName -> parameters.firstOrNull { it.name == paramName } }
    .filter { it != null }
    .map { it!! }
    .toList()

  private val queryParams = parameters - pathParams

  override val consumes: String
    get() {
      return bodyParameter?.type?.javaType?.mediaType() ?: MediaType.APPLICATION_JSON
    }

  override val parameterTypes: List<Type>
    get() = parameters.map { it.type.javaType }

  private val bodyParameter = parameters.subtract(pathParams).lastOrNull()

  override fun toSwaggerParams(): List<Parameter> {
    return if (this.parameters.isEmpty()) {
      return emptyList()
    } else {
      super.toSwaggerParams()
    }
  }

  override fun mapPathParameters(): List<PathParameter> {
    return pathParams.map { pathParam ->
      val swaggerProperty = pathParam.type.getSwaggerProperty()
      val p = PathParameter()
        .name(pathParam.name)
        .property(swaggerProperty)
        .type(swaggerProperty.type)
      pathParam.findAnnotation<DefaultValue>()?.apply {
        p.setDefaultValue(this.value)
      }
      p.required = true
      p
    }
  }

  override fun mapQueryParameters(): List<QueryParameter> {
    return queryParams.map { param ->
      val q = QueryParameter()
        .name(param.name)
        .property(param.type.getSwaggerProperty())
      param.findAnnotation<DefaultValue>()?.apply {
        q.setDefaultValue(this.value)
      }
      q.required = true
      if (param.isOptional) {
        q.minItems = 0
      }
      if (param.isVararg) {
        q.minItems = 0
      }
      q
    }
  }

  override fun mapBodyParameter(): BodyParameter? {
    return bodyParameter?.let {
      BodyParameter().apply {
        schema(bodyParameter.type.getSwaggerModelReference())
        setExamples(bodyParameter)
        name = bodyParameter.name
        required = true
      }
    }
  }

  private fun BodyParameter.setExamples(parameter: KParameter): BodyParameter {
    val example = parameter.findAnnotation<ApiParam>()?.example ?: return this
    this.example(HttpHeaderValues.APPLICATION_JSON.toString(), example)
    return this
  }
}