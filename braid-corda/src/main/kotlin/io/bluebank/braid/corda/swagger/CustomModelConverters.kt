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
package io.bluebank.braid.corda.swagger

import io.swagger.converter.ModelConverters
import io.swagger.jackson.AbstractModelConverter
import io.swagger.jackson.ModelResolver
import io.vertx.core.json.Json
import java.security.PublicKey

/**
 * To be used when calling BraidCordaJacksonInit.init()
 */
object CustomModelConverters {
    init {
        ModelConverters.getInstance().addConverter(CustomModelConverter())
    }

    fun init() {

    }
}