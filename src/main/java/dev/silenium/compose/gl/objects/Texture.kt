package dev.silenium.compose.gl.objects

import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.objects.TextureOrRenderbuffer.Companion.textureTargetBindings
import dev.silenium.compose.gl.util.checkGLError
import org.lwjgl.opengl.GL11.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

data class Texture(
    override val id: Int,
    override val size: IntSize,
    override val target: Int,
    override val internalFormat: Int,
) : TextureOrRenderbuffer {
    init {
        require(target in textureTargetBindings) { "Unsupported texture target: $target" }
    }

    override val binding: Int = textureTargetBindings[target]!!

    override fun bind() {
        glBindTexture(target, id)
        checkGLError("glBindTexture")
    }

    override fun unbind() {
        glBindTexture(target, 0)
        checkGLError("glBindTexture")
    }

    private val destroyed = AtomicBoolean(false)
    override fun destroy() {
        if (destroyed.compareAndExchange(false, true)) {
            glDeleteTextures(id)
        } else {
            logger.warn("Texture $id is already destroyed")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Texture::class.java)

        fun create(
            target: Int,
            size: IntSize,
            internalFormat: Int,
            wrapS: Int? = null,
            wrapT: Int? = null,
            minFilter: Int? = null,
            magFilter: Int? = null,
        ): Texture {
            val binding = textureTargetBindings[target]
            require(binding != null) { "Unsupported texture target: $target" }

            val id = glGenTextures()
            checkGLError("glGenTextures")
            check(id != 0) { "Failed to create texture" }

            val prev = glGetInteger(binding)
            checkGLError("glGetInteger")
            try {
                glBindTexture(target, id)
                checkGLError("glBindTexture")
                glTexImage2D(target, 0, internalFormat, size.width, size.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0)
                checkGLError("glTexImage2D")

                wrapS?.let {
                    glTexParameteri(target, GL_TEXTURE_WRAP_S, it)
                    checkGLError("glTexParameteri")
                }
                wrapT?.let {
                    glTexParameteri(target, GL_TEXTURE_WRAP_T, it)
                    checkGLError("glTexParameteri")
                }
                minFilter?.let {
                    glTexParameteri(target, GL_TEXTURE_MIN_FILTER, it)
                    checkGLError("glTexParameteri")
                }
                magFilter?.let {
                    glTexParameteri(target, GL_TEXTURE_MAG_FILTER, it)
                    checkGLError("glTexParameteri")
                }

                return Texture(id, size, target, internalFormat)
            } finally {
                glBindTexture(target, prev)
                checkGLError("glBindTexture")
            }
        }
    }
}
