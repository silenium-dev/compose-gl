package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.surface.GLDisplayScope
import dev.silenium.compose.gl.surface.GLDisplayScopeImpl
import dev.silenium.compose.gl.surface.GLDrawScope
import dev.silenium.compose.gl.surface.GLDrawScopeImpl
import org.lwjgl.egl.EGL15.eglGetError
import org.lwjgl.opengles.GLES
import org.lwjgl.opengles.GLES32.*
import java.nio.IntBuffer
import kotlin.time.Duration

class FBOPool(
    private val render: EGLContext,
    private val display: EGLContext,
    var size: IntSize,
    swapChainFactory: (Int, (IntSize) -> FBO) -> IFBOPresentMode,
    swapChainSize: Int = 10,
) {
    data class FBO(
        val id: Int,
        val colorAttachment: Int,
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

    init {
        ensureContext(ContextType.DISPLAY) {
            GLES.createCapabilities()
        }
    }

    private val swapChain: IFBOPresentMode = swapChainFactory(swapChainSize, ::createFBO)

    private enum class ContextType {
        RENDER,
        DISPLAY,
    }

    private fun <R> ensureContext(contextType: ContextType, block: () -> R): R {
        val previousContext = EGLContext.fromCurrent()
        val nextContext = when (contextType) {
            ContextType.RENDER -> render
            ContextType.DISPLAY -> display
        }

        nextContext.makeCurrent()
        return block().also {
            previousContext?.makeCurrent()
        }
    }

    fun initialize() = ensureContext(ContextType.RENDER) {
        println("Size: $size")
        swapChain.resize(size)
    }

    private fun createFBO(size: IntSize) = restoreAfter {
        val colorAttachment = glGenTextures()
        check(colorAttachment != 0) { "Failed to create color attachment: 0x${glGetError().toString(16).uppercase()}" }
        glBindTexture(GL_TEXTURE_2D, colorAttachment)
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_RGBA8,
            size.width,
            size.height,
            0,
            GL_RGBA,
            GL_UNSIGNED_BYTE,
            null as IntBuffer?
        )
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        val depthStencilAttachment = glGenRenderbuffers()
        check(depthStencilAttachment != 0) {
            "Failed to create depth/stencil attachment: 0x${eglGetError().toString(16).uppercase()}"
        }
        glBindRenderbuffer(GL_RENDERBUFFER, depthStencilAttachment)
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, size.width, size.height)

        val fbId = glGenFramebuffers()
        check(fbId != 0) { "Failed to create framebuffer: 0x${eglGetError().toString(16).uppercase()}" }
        glBindFramebuffer(GL_FRAMEBUFFER, fbId)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorAttachment, 0)
        glFramebufferRenderbuffer(
            GL_FRAMEBUFFER,
            GL_DEPTH_STENCIL_ATTACHMENT,
            GL_RENDERBUFFER,
            depthStencilAttachment
        )

        val status = glCheckFramebufferStatus(GL_FRAMEBUFFER)
        require(status == GL_FRAMEBUFFER_COMPLETE) {
            "Framebuffer is not complete: 0x${status.toString(16).uppercase()}"
        }

        FBO(fbId, colorAttachment, depthStencilAttachment, size)
    }

    private fun <R> restoreAfter(block: () -> R): R {
        if (!EGLContext.isCurrent()) return block()

        val prevFb = glGetInteger(GL_FRAMEBUFFER_BINDING)
        val prevReadFb = glGetInteger(GL_READ_FRAMEBUFFER_BINDING)
        val prevDrawFb = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING)
        val prevRb = glGetInteger(GL_RENDERBUFFER_BINDING)
        val prevTex = glGetInteger(GL_TEXTURE_BINDING_2D)
        val prevViewport = IntArray(4)
        glGetIntegerv(GL_VIEWPORT, prevViewport)
        val prevScissor = IntArray(4)
        glGetIntegerv(GL_SCISSOR_BOX, prevScissor)
        val prevDepthTest = glIsEnabled(GL_DEPTH_TEST)
        val prevStencilTest = glIsEnabled(GL_STENCIL_TEST)

        val result = block()

        glBindFramebuffer(GL_FRAMEBUFFER, prevFb)
        glBindFramebuffer(GL_READ_FRAMEBUFFER, prevReadFb)
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, prevDrawFb)
        glBindRenderbuffer(GL_RENDERBUFFER, prevRb)
        glBindTexture(GL_TEXTURE_2D, prevTex)
        glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3])
        glScissor(prevScissor[0], prevScissor[1], prevScissor[2], prevScissor[3])
        if (prevDepthTest) glEnable(GL_DEPTH_TEST) else glDisable(GL_DEPTH_TEST)
        if (prevStencilTest) glEnable(GL_STENCIL_TEST) else glDisable(GL_STENCIL_TEST)

        return result
    }

    /**
     * @return wait time for the next frame
     */
    fun render(deltaTime: Duration, block: GLDrawScope.() -> Unit): Duration? = swapChain.render { fbo ->
        ensureContext(ContextType.RENDER) {
            if (fbo.size != size) ensureContext(ContextType.RENDER) {
                swapChain.resize(size)
            }
            fbo.bind()
            val drawScope = GLDrawScopeImpl(fbo, deltaTime)
            drawScope.block()
            glFinish()
            fbo.unbind()

            drawScope.redrawAfter
        }
    }

    fun display(block: GLDisplayScope.() -> Unit) = swapChain.display { fbo ->
        ensureContext(ContextType.DISPLAY) {
            val displayScope = GLDisplayScopeImpl(fbo)
            displayScope.block()
        }
    }

    fun destroy() = ensureContext(ContextType.RENDER) {
        swapChain.destroyFBOs()
    }
}
