package dev.silenium.compose.gl.util

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*

@OptIn(ExperimentalPathApi::class)
object Natives {
    private val dir = Files.createTempDirectory("compose-gl-natives")
    private val libs = mutableMapOf<String, Path>()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            dir.deleteRecursively()
        })
    }

    // TODO: Support different os and architectures
    fun load(libFileName: String) {
        val libFile = libs.getOrPut(libFileName) {
            val outputFile = dir.resolve(libFileName)
            outputFile.createParentDirectories()
            Natives::class.java.classLoader.getResourceAsStream("natives/$libFileName")!!.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outputFile
        }
        System.load(libFile.absolutePathString())
    }
}
