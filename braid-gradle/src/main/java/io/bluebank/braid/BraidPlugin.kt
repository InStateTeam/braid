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

                    val url = URL("https://repo1.maven.org/maven2/io/bluebank/braid/braid-server/4.1.2-RC08/braid-server-4.1.2-RC08.jar")
                    Channels.newChannel(url.openStream()).use { rbc ->
                        FileOutputStream("${project.buildDir}/braid/braid.jar")
                            .use { fos ->
                                fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE);
                            }
                    }
                }

    }

    val executableFileMode = "0755".toInt(8)
}


fun Project.getPluginFile(filePathInJar: String): File {
    val tmpDir = File(this.buildDir, "tmp")
    tmpDir.mkdirs()
    val outputFile = File(tmpDir, filePathInJar)
    outputFile.outputStream().use {
        BraidPlugin::class.java.getResourceAsStream(filePathInJar).copyTo(it)
    }
    return outputFile
}