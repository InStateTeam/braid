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
package io.bluebank.braid.core.jsonrpc

import java.lang.reflect.Constructor
import kotlin.reflect.KFunction

data class JsonRPCRequest(val jsonrpc: String = "2.0", val id: Long, val method: String, val params: Any?, val streamed: Boolean = false) {
  private val parameters = Params.build(params)

  fun paramCount() : Int = parameters.count

  fun matchesName(method: KFunction<*>): Boolean = method.name == this.method

  fun mapParams(method: KFunction<*>): Array<Any?> {
    return parameters.mapParams(method).toTypedArray()
  }

  fun mapParams(constructor: Constructor<*>) : Array<Any?> {
    return parameters.mapParams(constructor).toTypedArray()
  }

  fun paramsAsString() = parameters.toString()
}