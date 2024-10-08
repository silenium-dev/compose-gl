package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.context.GLContext
import dev.silenium.compose.gl.objects.Renderbuffer
import dev.silenium.compose.gl.objects.Texture
import dev.silenium.compose.gl.surface.GLDisplayScope
import dev.silenium.compose.gl.surface.GLDisplayScopeImpl
import dev.silenium.compose.gl.surface.GLDrawScope
import dev.silenium.compose.gl.surface.GLDrawScopeImpl
import kotlinx.coroutines.CancellationException
import org.lwjgl.opengl.GL30.*
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
    ) : this(render, display, size, swapChainFactory(swapChainSize, ::createFBO))

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
        try {
            return block()
        } finally {
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
    fun render(deltaTime: Duration, block: GLDrawScope.() -> Unit): Result<Duration?> = try {
        ensureContext(ContextType.RENDER) {
            if (swapChain.size != size) {
                swapChain.resize(size)
            }
            restoreAfter {
                swapChain.render { fbo ->
                    fbo.bind()
                    val drawScope = GLDrawScopeImpl(fbo, deltaTime)
                    drawScope.block()
                    glFlush()
                    fbo.unbind()

                    if (drawScope.terminate) {
                        Result.failure(CancellationException("Rendering terminated"))
                    } else {
                        Result.success(drawScope.redrawAfter)
                    }
                }
            } ?: Result.failure(NoRenderFBOAvailable())
        }
    } catch (t: Throwable) {
        Result.failure(t)
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
            val colorAttachment = Texture.create(
                GL_TEXTURE_2D, size, GL_RGBA8,
                GL_CLAMP_TO_EDGE, GL_CLAMP_TO_EDGE, GL_LINEAR, GL_LINEAR,
            )
            val depthStencilAttachment = Renderbuffer.create(size, GL_DEPTH24_STENCIL8)

            FBO.create(colorAttachment, depthStencilAttachment)
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

            try {
                return block()
            } finally {
                glBindFramebuffer(GL_FRAMEBUFFER, prevFb)
                glBindFramebuffer(GL_READ_FRAMEBUFFER, prevReadFb)
                glBindFramebuffer(GL_DRAW_FRAMEBUFFER, prevDrawFb)
                glBindRenderbuffer(GL_RENDERBUFFER, prevRb)
                glBindTexture(GL_TEXTURE_2D, prevTex)
                glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3])
                glScissor(prevScissor[0], prevScissor[1], prevScissor[2], prevScissor[3])
                if (prevDepthTest) glEnable(GL_DEPTH_TEST) else glDisable(GL_DEPTH_TEST)
                if (prevStencilTest) glEnable(GL_STENCIL_TEST) else glDisable(GL_STENCIL_TEST)
            }
        }
    }
}
