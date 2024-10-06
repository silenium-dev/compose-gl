package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import org.lwjgl.opengl.GL30.*

data class FBO(
    val id: Int,
    /**
     * The color attachment texture
     */
    val colorAttachment: Int,
    /**
     * The depth/stencil attachment renderbuffer
     */
    val depthStencilAttachment: Int,
    val size: IntSize,
) {
    fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, id)
        glViewport(0, 0, size.width, size.height)
    }

    fun unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    fun destroy() {
        glDeleteTextures(colorAttachment)
        glDeleteRenderbuffers(depthStencilAttachment)
        glDeleteFramebuffers(id)
    }
}
