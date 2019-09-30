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
import java.net.URL
import java.net.URLClassLoader
import java.util.Arrays.asList

object PathsClassLoader {
  private val tempFileDownloader = JarDownloader()

  fun jarsClassLoader(vararg jarPaths: String) =
    jarsClassLoader(jarPaths.toList())

  fun jarsClassLoader(jarPaths: Collection<String>): ClassLoader {
    val urls = jarPaths.asSequence().map {
      urlOrFiles(it)
    }.flatMap {it.asSequence()}.toList().toTypedArray()
    return URLClassLoader(urls, Thread.currentThread().contextClassLoader)
  }

  fun urlOrFiles(urlOrFileName: String): List<URL> {
    return try {
      // attempt to download the file if available
      asList(tempFileDownloader.uriToFile(URI(urlOrFileName).toURL()))
    } catch (err: IllegalArgumentException) {
      localFiles(urlOrFileName)
    }
  }

 private fun localFiles(urlOrFileName: String): List<URL> {
      // attempt to create as a path
      val file = File(urlOrFileName)
      if(file.isDirectory){
           return file.list().map { localFiles(it) }.flatMap { it }.toList()
      }
      return asList(file.toURI().toURL())
    }
}

fun List<String>.toJarsClassLoader(): ClassLoader {
  return PathsClassLoader.jarsClassLoader(this.toList())
}
