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
package io.bluebank.braid.server.util

import java.net.URL
import java.net.URLEncoder

fun URLEncoder.utf8(value:String):String{
    return URLEncoder.encode(value,"UTF-8")
}

private val CORDAPP_NAME_RE = "(.+)(\\-[0-9](\\.[0-9])*\\.jar)?".toRegex()

fun URL.getCordappName() : String {
    return "test-cordapp"
//    val fileName = File(this.file).name
//    val matches = CORDAPP_NAME_RE.matchEntire(file)
//    return when (matches) {
//        null -> error("parsing of cordapp module location failed: $this")
//        else -> matches.groupValues[2]
//    }
}