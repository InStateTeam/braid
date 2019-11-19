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
package io.bluebank.braid

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

open class BraidPlugin : Plugin<Project> {
  companion object {
    val repo = "https://repo1.maven.org/maven2"
  }

  override fun apply(project: Project) {
    val extension = project.extensions
        .create("braid", BraidPluginExtension::class.java)

    project.buildDir
    project.task("braid")
        .doLast {
          project.copy {
            it.from(project.getPluginFile("/braid.bat"))
            it.into("${project.buildDir}/braid")
          }

          project.copy {
            it.from(project.getPluginFile("/braid"))
            it.fileMode = executableFileMode
            it.into("${project.buildDir}/braid")
          }

          val latest = ManifestReader("$repo/io/bluebank/braid/braid-server/maven-metadata.xml").latest()
          val url = URL("$repo/io/bluebank/braid/braid-server/$latest/braid-server-$latest.jar")
          url.downloadTo("${project.buildDir}/braid/braid.jar")


          // <node address> <username> <password> <port> <openApiVersion> [<cordaAppJar1> <cordAppJar2>
          File("${project.buildDir}/braid/startBraid.bat")
              .writeText("braid.bat ${extension.networkAndPort} ${extension.username} ${extension.password} ${extension.port} 2 ${extension.cordAppsDirectory}")

          val file = File("${project.buildDir}/braid/startBraid")
          file.writeText("braid.bat ${extension.networkAndPort} ${extension.username} ${extension.password} ${extension.port} 2 ${extension.cordAppsDirectory}")
          file.setWritable(true)
          file.setExecutable(true, false)
          file.setReadable(true,false)
        }

  }


  val executableFileMode = "0755".toInt(8)
}


private fun Project.getPluginFile(filePathInJar: String): File {
  val tmpDir = File(this.buildDir, "tmp")
  tmpDir.mkdirs()
  val outputFile = File(tmpDir, filePathInJar)
  outputFile.outputStream().use {
    BraidPlugin::class.java.getResourceAsStream(filePathInJar).copyTo(it)
  }
  return outputFile
}

private fun URL.downloadTo(destination: String) {
  Channels.newChannel(this.openStream()).use { rbc ->
    FileOutputStream(destination).use { fos ->
          fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE);
        }
  }
}
