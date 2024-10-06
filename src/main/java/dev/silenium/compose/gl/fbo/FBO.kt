package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.objects.Renderbuffer
import dev.silenium.compose.gl.objects.Texture
import dev.silenium.compose.gl.util.checkGLError
import org.lwjgl.opengl.GL30.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

data class FBO(
    val id: Int,
    val size: IntSize,
    val colorAttachment: Texture,
    val depthStencilAttachment: Renderbuffer,
) {
    fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, id)
        checkGLError("glBindFramebuffer")
        glViewport(0, 0, size.width, size.height)
    }

    fun unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        checkGLError("glBindFramebuffer")
    }

    private val destroyed = AtomicBoolean(false)
    fun destroy() {
        if (destroyed.compareAndExchange(false, true)) {
            glDeleteFramebuffers(id)
            colorAttachment.destroy()
            depthStencilAttachment.destroy()
        } else {
            logger.trace("FBO $id is already destroyed", Exception())
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FBO::class.java)

        fun create(
            colorAttachment: Texture,
            depthStencilAttachment: Renderbuffer,
            textureLevel: Int = 0,
        ): FBO {
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

                return FBO(fbId, colorAttachment.size, colorAttachment, depthStencilAttachment)
            } finally {
                glBindFramebuffer(GL_FRAMEBUFFER, prev)
                checkGLError("glBindFramebuffer")
            }
        }
    }
}
