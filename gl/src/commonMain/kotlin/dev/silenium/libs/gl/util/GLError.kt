package dev.silenium.libs.gl.util

import dev.silenium.libs.gl.GLProvider.GL_NO_ERROR
import dev.silenium.libs.gl.GLProvider.glGetError

data class GLError(val error: Int, val operation: String? = null) :
    Exception(
        "${operation?.let { "$it: " }.orEmpty()}OpenGL error: 0x${error.toString(16).uppercase()}"
    )

fun checkGLError(operation: String? = null) {
    val error = glGetError()
    if (error != GL_NO_ERROR) {
        throw GLError(error, operation)
    }
}
