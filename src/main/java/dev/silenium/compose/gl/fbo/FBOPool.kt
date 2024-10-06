package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.context.GLContext
import dev.silenium.compose.gl.surface.GLDisplayScope
import dev.silenium.compose.gl.surface.GLDisplayScopeImpl
import dev.silenium.compose.gl.surface.GLDrawScope
import dev.silenium.compose.gl.surface.GLDrawScopeImpl
import org.lwjgl.opengl.GL30.*
import java.nio.IntBuffer
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.time.Duration

/**
 * A pool of framebuffers.
 * @param render The OpenGL context for rendering.
 * @param display The OpenGL context for displaying.
 * @param size The initial size of the framebuffers.
 * @param swapChain The swap chain for the framebuffers.
 */
class FBOPool(
    private val render: GLContext<*>,
    private val display: GLContext<*>,
    var size: IntSize,
    private val swapChain: FBOSwapChain,
) {

    /**
     * A pool of framebuffers.
     * @param render The OpenGL context for rendering.
     * @param display The OpenGL context for displaying.
     * @param size The initial size of the framebuffers.
     * @param swapChainFactory The factory for creating the swap chain.
     * @param swapChainSize The size of the swap chain.
     */
    constructor(
        render: GLContext<*>,
        display: GLContext<*>,
        size: IntSize,
        swapChainFactory: (Int, (IntSize) -> FBO) -> FBOSwapChain,
        swapChainSize: Int = 10,
    ): this(render, display, size, swapChainFactory(swapChainSize, ::createFBO))

    private enum class ContextType {
        RENDER,
        DISPLAY,
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun <R> ensureContext(contextType: ContextType, block: () -> R): R {
        contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
        val previousContext = render.provider.fromCurrent()
        val nextContext = when (contextType) {
            ContextType.RENDER -> render
            ContextType.DISPLAY -> display
        }

        nextContext.makeCurrent()
        return block().also {
            previousContext?.makeCurrent()
        }
    }

    /**
     * Initialize the framebuffers, has to be called once in the render context before it can be used.
     */
    fun initialize() = ensureContext(ContextType.RENDER) {
        swapChain.resize(size)
    }

    /**
     * Render the next frame, has to be called in the render context.
     * @param deltaTime The time since the last frame.
     * @param block The block to render the frame.
     * @return wait time for the next frame, or null, if there was no frame rendered due to no framebuffers being available.
     */
    suspend fun render(deltaTime: Duration, block: suspend GLDrawScope.() -> Unit): Duration? =
        ensureContext(ContextType.RENDER) {
            if (swapChain.size != size) {
                swapChain.resize(size)
            }
            restoreAfter {
                swapChain.render { fbo ->
                    fbo.bind()
                    val drawScope = GLDrawScopeImpl(fbo, deltaTime)
                    drawScope.block()
                    glFinish()
                    fbo.unbind()

                    drawScope.redrawAfter
                }
            }
        }

    /**
     * Display the next frame, has to be called in the display context.
     * @param block The block to display the frame.
     */
    fun display(block: GLDisplayScope.() -> Unit) = swapChain.display { fbo ->
        ensureContext(ContextType.DISPLAY) {
            val displayScope = GLDisplayScopeImpl(fbo)
            displayScope.block()
        }
    }

    /**
     * Destroy the framebuffers, has to be called once in the render context after it is no longer needed.
     * This will destroy all framebuffers.
     * @see [FBOSwapChain.destroyFBOs]
     */
    fun destroy() = ensureContext(ContextType.RENDER) {
        swapChain.destroyFBOs()
    }

    companion object {
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
                "Failed to create depth/stencil attachment: 0x${glGetError().toString(16).uppercase()}"
            }
            glBindRenderbuffer(GL_RENDERBUFFER, depthStencilAttachment)
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH24_STENCIL8, size.width, size.height)

            val fbId = glGenFramebuffers()
            check(fbId != 0) { "Failed to create framebuffer: 0x${glGetError().toString(16).uppercase()}" }
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

        @OptIn(ExperimentalContracts::class)
        private inline fun <R> restoreAfter(block: () -> R): R {
            contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

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
    }
}
