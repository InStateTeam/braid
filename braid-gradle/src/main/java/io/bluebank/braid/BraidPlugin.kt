package io.bluebank.braid

import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

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
                }
//
//        project.copy {
//            it.from(project.configurations.getByName("runtime"))
//            it.into("${project.buildDir}/braid/libs")
//        }
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