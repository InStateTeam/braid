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
package io.bluebank.braid.server

import io.bluebank.braid.server.flow.StartableByRPCFinder
import io.bluebank.braid.server.util.PathsClassLoader

fun main(args: Array<String>) {
  val urls = listOf(
    "https://repo1.maven.org/maven2/net/corda/corda-finance-contracts/4.0/corda-finance-contracts-4.0.jar",
    "https://repo1.maven.org/maven2/net/corda/corda-finance-workflows/4.0/corda-finance-workflows-4.0.jar"
  )
//
//  val dir = Files.createTempDirectory("delete").toFile().also { it.deleteOnExit() }
//  val localFiles = urls.map { uriString ->
//    val uri = URI(uriString)
//    val filename = uri.path.let { File(it) }.name
//    val dst = File(dir, filename)
//    Channels.newChannel(uri.toURL().openStream()).use { rbc ->
//      FileOutputStream(dst).use { fos ->
//        fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE);
//      }
//    }
//    dst.toString()
//  }

  val classLoader = PathsClassLoader.cordappsClassLoader(urls)
  val classes = StartableByRPCFinder(classLoader).findStartableByRPC();
  println(classes)
}