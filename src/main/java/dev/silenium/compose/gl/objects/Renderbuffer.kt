package dev.silenium.compose.gl.objects

import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.util.checkGLError
import org.lwjgl.opengl.GL30.*

data class Renderbuffer(
    override val id: Int,
    override val size: IntSize,
    override val internalFormat: Int,
) : TextureOrRenderbuffer {
    override val target: Int = GL_RENDERBUFFER
    override val binding: Int = GL_RENDERBUFFER_BINDING

    override fun bind() {
        glBindRenderbuffer(GL_RENDERBUFFER, id)
        checkGLError("glBindRenderbuffer")
    }

    override fun unbind() {
        glBindRenderbuffer(GL_RENDERBUFFER, 0)
        checkGLError("glBindRenderbuffer")
    }

    override fun destroy() {
        glDeleteRenderbuffers(id)
    }

    companion object {
        fun create(size: IntSize, internalFormat: Int): Renderbuffer {
            val id = glGenRenderbuffers()
            checkGLError("glGenRenderbuffers")
            check(id != 0) { "Failed to create renderbuffer" }

            val prev = glGetInteger(GL_RENDERBUFFER_BINDING)
            checkGLError("glGetInteger")
            try {
                glBindRenderbuffer(GL_RENDERBUFFER, id)
                checkGLError("glBindRenderbuffer")

                glRenderbufferStorage(GL_RENDERBUFFER, internalFormat, size.width, size.height)
                checkGLError("glRenderbufferStorage")
                return Renderbuffer(id, size, internalFormat)
            } finally {
                glBindRenderbuffer(GL_RENDERBUFFER, prev)
                checkGLError("glBindRenderbuffer")
            }
        }
    }
}
