package dev.silenium.compose.gl.util

import org.lwjgl.opengl.GL11.GL_NO_ERROR
import org.lwjgl.opengl.GL11.glGetError

data class GLError(val error: Int, val operation: String? = null) :
    Exception("${operation?.let { "$it: " }.orEmpty()}OpenGL error: 0x${error.toString(16).uppercase()}")

fun checkGLError(operation: String? = null) {
    val error = glGetError()
    if (error != GL_NO_ERROR) {
        throw GLError(error, operation)
    }
}
