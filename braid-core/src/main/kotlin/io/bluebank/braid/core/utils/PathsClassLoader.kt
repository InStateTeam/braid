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
package io.bluebank.braid.core.utils

import java.io.File
import java.net.URI
import java.net.URLClassLoader

object PathsClassLoader {
  fun cordappsClassLoader(vararg paths: String) =
    cordappsClassLoader(paths.toList())

  fun cordappsClassLoader(args: Collection<String>): ClassLoader {
    val toArray = args.asSequence().map {
      try {
        // creating as a web url
        URI(it).toURL()
      } catch (err: IllegalArgumentException) {
        // attempt to create as a path
        File(it).toURI().toURL()
      }
    }.toList().toTypedArray()
    return URLClassLoader(toArray, Thread.currentThread().contextClassLoader)
  }
}

fun List<String>.toCordappsClassLoader(): ClassLoader {
  return PathsClassLoader.cordappsClassLoader(this.toList())
}

fun <R> tryWithClassLoader(classLoader: ClassLoader, fn: () -> R): R {
  val cachedClassLoader = Thread.currentThread().contextClassLoader
  Thread.currentThread().contextClassLoader = classLoader
  return try {
    fn()
  } finally {
    Thread.currentThread().contextClassLoader = cachedClassLoader
  }
}