package dev.silenium.libs.gl.objects

import androidx.compose.ui.unit.IntSize
import dev.silenium.libs.foreign.ext.DoubleDestructionProtection
import dev.silenium.libs.gl.GLProvider.GL_RENDERBUFFER
import dev.silenium.libs.gl.GLProvider.GL_RENDERBUFFER_BINDING
import dev.silenium.libs.gl.GLProvider.glBindRenderbuffer
import dev.silenium.libs.gl.GLProvider.glDeleteRenderbuffers
import dev.silenium.libs.gl.GLProvider.glGenRenderbuffers
import dev.silenium.libs.gl.GLProvider.glGetInteger
import dev.silenium.libs.gl.GLProvider.glRenderbufferStorage
import dev.silenium.libs.gl.util.checkGLError

data class Renderbuffer(
    override val value: Int,
    override val size: IntSize,
    override val internalFormat: Int,
) : TextureOrRenderbuffer<Renderbuffer>, DoubleDestructionProtection<Int>() {
    override val id: Int by ::value
    override val target: Int = GL_RENDERBUFFER
    override val binding: Int = GL_RENDERBUFFER_BINDING

    override fun bind() {
        glBindRenderbuffer(GL_RENDERBUFFER, value)
        checkGLError("glBindRenderbuffer")
    }

    override fun unbind() {
        glBindRenderbuffer(GL_RENDERBUFFER, 0)
        checkGLError("glBindRenderbuffer")
    }

    override fun destroyInternal() {
        glDeleteRenderbuffers(value)
    }

    @Synchronized
    override fun resize(size: IntSize): Renderbuffer {
        val prev = glGetInteger(binding)
        checkGLError("glGetInteger")
        try {
            bind()

            glRenderbufferStorage(GL_RENDERBUFFER, internalFormat, size.width, size.height)
            checkGLError("glRenderbufferStorage")
            return copy(size = size).also { this.abandon() }
        } finally {
            glBindRenderbuffer(GL_RENDERBUFFER, prev)
            checkGLError("glBindRenderbuffer")
        }
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
