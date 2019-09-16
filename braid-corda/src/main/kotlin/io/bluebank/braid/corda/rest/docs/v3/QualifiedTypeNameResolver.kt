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
package io.bluebank.braid.corda.rest.docs.v3

import io.swagger.v3.core.jackson.TypeNameResolver
import io.swagger.v3.oas.annotations.media.Schema

/**
 * A specialised type resolver that preserves package names
 */
class QualifiedTypeNameResolver : TypeNameResolver() {

  override fun nameForClass(cls: Class<*>, options: MutableSet<Options>): String {
    val className = when {
      cls.name.startsWith("java.") -> cls.simpleName
      else -> cls.name
    }
    if (options.contains(Options.SKIP_API_MODEL)) {
      return className
    }
    return cls.getAnnotation(Schema::class.java)?.name?.trimToNull() ?: className
  }
}

private fun String.trimToNull(): String? {
  val trimmed = this.trim()
  return when {
    trimmed.isEmpty() -> null
    else -> trimmed
  }
}
