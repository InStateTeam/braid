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

package io.bluebank.braid.corda.restafarian.docs

import io.swagger.models.Operation
import io.swagger.models.parameters.Parameter
import io.vertx.core.http.HttpMethod

class ImplicitParamsEndPoint(override val groupName: String, override val method: HttpMethod, override val path: String) : EndPoint() {
  override val description: String
    get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

  override fun decorateOperationWithResponseType(operation: Operation) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun toSwaggerParams(): List<Parameter> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
