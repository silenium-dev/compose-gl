package dev.silenium.compose.gl.util

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteRecursively
import kotlin.io.path.outputStream

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
        val libFile = libs.getOrPut(libFileName)     {
            val outputFile = dir.resolve(libFileName)
            Natives::class.java.getResourceAsStream("/natives/$libFileName")!!.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outputFile
        }
        System.load(libFile.absolutePathString())
    }
}
