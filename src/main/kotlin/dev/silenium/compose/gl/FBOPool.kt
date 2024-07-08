package dev.silenium.compose.gl

import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.surface.GLDisplayScope
import dev.silenium.compose.gl.surface.GLDisplayScopeImpl
import dev.silenium.compose.gl.surface.GLDrawScope
import dev.silenium.compose.gl.surface.GLDrawScopeImpl
import dev.silenium.compose.gl.util.snapshot
import org.lwjgl.egl.EGL
import org.lwjgl.egl.EGL15.*
import org.lwjgl.egl.EGLCapabilities
import org.lwjgl.opengles.GLES
import org.lwjgl.opengles.GLES32.*
import org.lwjgl.system.MemoryUtil
import java.nio.IntBuffer
import java.util.concurrent.ArrayBlockingQueue
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.io.path.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class EGLContext(
    val display: Long,
    val context: Long,
    val surface: Long,
) {
    var capabilities: EGLCapabilities
        private set

    init {
        restorePrevious {
            makeCurrent()
            capabilities = EGL.createDisplayCapabilities(display)
            GLES.createCapabilities()
        }
    }

    fun makeCurrent() {
        eglMakeCurrent(display, surface, surface, context)
    }

    fun swapBuffers() {
        eglSwapBuffers(display, surface)
    }

    fun destroy() {
        eglDestroyContext(display, context)
        eglDestroySurface(display, surface)
    }

    companion object {
        @OptIn(ExperimentalContracts::class)
        private fun <R> restorePrevious(block: () -> R): R {
            contract { callsInPlace(block, kotlin.contracts.InvocationKind.EXACTLY_ONCE) }
            val display = eglGetCurrentDisplay()
            val context = eglGetCurrentContext()
            val surface = eglGetCurrentSurface(EGL_DRAW)
            if (display == EGL_NO_DISPLAY || context == EGL_NO_CONTEXT || surface == EGL_NO_SURFACE) {
                return block()
            }
            return block().also {
                eglMakeCurrent(display, surface, surface, context)
            }
        }

        fun fromCurrent(): EGLContext? {
            val display = eglGetCurrentDisplay()
            val context = eglGetCurrentContext()
            val surface = eglGetCurrentSurface(EGL_DRAW)
            if (display == EGL_NO_DISPLAY || context == EGL_NO_CONTEXT || surface == EGL_NO_SURFACE) {
                return null
            }
            return EGLContext(display, context, surface)
        }

        fun isCurrent(): Boolean {
            val display = eglGetCurrentDisplay()
            val context = eglGetCurrentContext()
            val surface = eglGetCurrentSurface(EGL_DRAW)
            return display != EGL_NO_DISPLAY && context != EGL_NO_CONTEXT && surface != EGL_NO_SURFACE
        }

        fun createOffscreen(parent: EGLContext): EGLContext {
            val display = parent.display
            check(eglBindAPI(EGL_OPENGL_ES_API)) { "Failed to bind API: 0x${eglGetError().toString(16).uppercase()}" }

            val attribList = intArrayOf(
                EGL_RED_SIZE,
                8,
                EGL_GREEN_SIZE,
                8,
                EGL_BLUE_SIZE,
                8,
                EGL_ALPHA_SIZE,
                8,
                EGL_RENDERABLE_TYPE,
                EGL_OPENGL_ES3_BIT,
                EGL_NONE
            )
            val config = MemoryUtil.memAllocPointer(1)
            val numConfig = IntArray(1)
            if (!eglChooseConfig(display, attribList, config, numConfig)) {
                error("Failed to choose config: 0x${eglGetError().toString(16).uppercase()}")
            }
            val context = eglCreateContext(
                display,
                config.get(0),
                parent.context,
                intArrayOf(
                    EGL_CONTEXT_MAJOR_VERSION, 3,
                    EGL_CONTEXT_MINOR_VERSION, 2,
                    EGL_NONE
                )
            )
            check(context != EGL_NO_CONTEXT) { "Failed to create context: 0x${eglGetError().toString(16).uppercase()}" }
            val surface =
                eglCreatePbufferSurface(display, config.get(0), intArrayOf(EGL_WIDTH, 1, EGL_HEIGHT, 1, EGL_NONE))
            check(surface != EGL_NO_SURFACE) { "Failed to create surface: 0x${eglGetError().toString(16).uppercase()}" }
            val result = EGLContext(display, context, surface)

            return restorePrevious {
                result.makeCurrent()
                GLES.createCapabilities()
                result
            }
        }
    }
}

class FBOPool(
    val render: EGLContext,
    val display: EGLContext,
    var size: IntSize
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

    private val available = ArrayBlockingQueue<FBO>(24)
    private val toDisplay = ArrayBlockingQueue<FBO>(24)

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
        repeat(available.remainingCapacity()) {
            available.add(createFBO(size))
        }
    }

    private fun createFBO(size: IntSize) = restoreAfter {
        val context = EGLContext.fromCurrent()
        println("Context: $context")
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
    fun render(deltaTime: Duration, block: GLDrawScope.() -> Unit): Duration? {
        println("FBOs: ${available.size}, ${toDisplay.size}")
        val fbo: FBO = available.poll() ?: return null
        val waitTime = ensureContext(ContextType.RENDER) {
            fbo.bind()
            val drawScope = GLDrawScopeImpl(fbo, deltaTime)
            drawScope.block()
            glFinish()
            fbo.unbind()

            drawScope.redrawAfter
        }
        toDisplay.add(fbo)
        return waitTime
    }

    fun display(block: GLDisplayScope.() -> Unit) {
        if (toDisplay.size > 1) {
            if (available.offer(toDisplay.peek())) toDisplay.poll()
        }
        val fbo = toDisplay.peek() ?: return
        ensureContext(ContextType.DISPLAY) {
            val displayScope = GLDisplayScopeImpl(fbo)
            displayScope.block()
        }
        if (fbo.size != size) ensureContext(ContextType.RENDER) {
            toDisplay.poll()
            fbo.destroy()
            available.add(createFBO(size))

            if (available.any { it.size == size }) {
                val copy = available.filter { it.size != size }
                available.removeIf { it.size != size }
                copy.forEach {
                    it.destroy()
                    available.add(createFBO(size))
                }
            }
        }
    }

    fun destroy() = ensureContext(ContextType.RENDER) {
        while (available.isNotEmpty()) available.poll().destroy()
        while (toDisplay.isNotEmpty()) toDisplay.poll().destroy()
    }
}
