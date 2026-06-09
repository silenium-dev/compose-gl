package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.GLProvider.GL_COLOR_ATTACHMENT0
import dev.silenium.compose.gl.GLProvider.GL_DEPTH_STENCIL_ATTACHMENT
import dev.silenium.compose.gl.GLProvider.GL_FRAMEBUFFER
import dev.silenium.compose.gl.GLProvider.GL_FRAMEBUFFER_BINDING
import dev.silenium.compose.gl.GLProvider.GL_FRAMEBUFFER_COMPLETE
import dev.silenium.compose.gl.GLProvider.GL_RENDERBUFFER
import dev.silenium.compose.gl.GLProvider.GL_TEXTURE_2D
import dev.silenium.compose.gl.GLProvider.glBindFramebuffer
import dev.silenium.compose.gl.GLProvider.glCheckFramebufferStatus
import dev.silenium.compose.gl.GLProvider.glDeleteFramebuffers
import dev.silenium.compose.gl.GLProvider.glFramebufferRenderbuffer
import dev.silenium.compose.gl.GLProvider.glFramebufferTexture2D
import dev.silenium.compose.gl.GLProvider.glGenFramebuffers
import dev.silenium.compose.gl.GLProvider.glGetInteger
import dev.silenium.compose.gl.GLProvider.glViewport
import dev.silenium.compose.gl.objects.Renderbuffer
import dev.silenium.compose.gl.objects.Texture
import dev.silenium.compose.gl.util.DoubleDestructionProtection
import dev.silenium.compose.gl.util.checkGLError

sealed class FBO : DoubleDestructionProtection<Int>() {
    abstract val size: IntSize
    abstract override val id: Int
    protected var destroySkikoCompatible = false

    open fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, id)
        checkGLError("glBindFramebuffer")
        glViewport(0, 0, size.width, size.height)
    }

    open fun unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        checkGLError("glBindFramebuffer")
    }

    abstract override fun destroyInternal()

    @Synchronized
    fun destroySkikoCompatible() {
        destroySkikoCompatible = true
        destroy()
    }

    data class Indestructible(override val id: Int, override val size: IntSize) : FBO() {
        override fun destroyInternal() = Unit
        override fun bind() {
            glViewport(0, 0, size.width, size.height)
        }
        override fun unbind() = Unit
    }

    data class Custom(
        override val id: Int,
        override val size: IntSize,
        val colorAttachment: Texture,
        val depthStencilAttachment: Renderbuffer,
    ) : FBO() {
        override fun destroyInternal() {
            glDeleteFramebuffers(id)
            if (destroySkikoCompatible) {
                colorAttachment.abandon()
            } else {
                colorAttachment.destroy()
            }
            depthStencilAttachment.destroy()
        }
    }

    companion object {
        fun create(
            colorAttachment: Texture,
            depthStencilAttachment: Renderbuffer,
            textureLevel: Int = 0,
        ): Custom {
            require(colorAttachment.size == depthStencilAttachment.size) {
                "Color attachment and depth/stencil attachment must have the same size"
            }

            val fbId = glGenFramebuffers()
            checkGLError("glGenFramebuffers")
            check(fbId != 0) { "Failed to create framebuffer" }

            val prev = glGetInteger(GL_FRAMEBUFFER_BINDING)
            checkGLError("glGetInteger")

            try {
                glBindFramebuffer(GL_FRAMEBUFFER, fbId)
                checkGLError("glBindFramebuffer")
                glFramebufferTexture2D(
                    GL_FRAMEBUFFER,
                    GL_COLOR_ATTACHMENT0,
                    GL_TEXTURE_2D,
                    colorAttachment.id,
                    textureLevel,
                )
                checkGLError("glFramebufferTexture2D")

                glFramebufferRenderbuffer(
                    GL_FRAMEBUFFER,
                    GL_DEPTH_STENCIL_ATTACHMENT,
                    GL_RENDERBUFFER,
                    depthStencilAttachment.id,
                )
                checkGLError("glFramebufferRenderbuffer")

                val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
                checkGLError("glCheckFramebufferStatus")
                require(status == GL_FRAMEBUFFER_COMPLETE) {
                    "Framebuffer is not complete: 0x${status.toString(16).uppercase()}"
                }

                return Custom(fbId, colorAttachment.size, colorAttachment, depthStencilAttachment)
            } finally {
                glBindFramebuffer(GL_FRAMEBUFFER, prev)
                checkGLError("glBindFramebuffer")
            }
        }
    }
}

data class FBODrawScope(val fbo: FBO)

inline fun <T> FBO.draw(block: FBODrawScope.() -> T): T {
    bind()
    try {
        val scope = FBODrawScope(this)
        return scope.block()
    } finally {
        unbind()
    }
}
