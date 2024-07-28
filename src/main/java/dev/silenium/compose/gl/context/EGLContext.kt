package dev.silenium.compose.gl.context

import org.lwjgl.egl.EGL
import org.lwjgl.egl.EGL15.*
import org.lwjgl.egl.EGLCapabilities
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.system.MemoryUtil

data class EGLContext(
    val display: Long,
    val context: Long,
    val surface: Long,
) : GLContext {
    lateinit var eglCapabilities: EGLCapabilities
        private set
    override lateinit var glCapabilities: GLCapabilities
        private set

    init {
        restorePrevious {
            makeCurrent()
            eglCapabilities = EGL.createDisplayCapabilities(display)
            glCapabilities = GL.createCapabilities()
        }
    }

    override fun unbindCurrent() {
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
    }

    override fun makeCurrent() {
        eglMakeCurrent(display, surface, surface, context)
    }

    override fun destroy() {
        eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
        eglDestroyContext(display, context)
        eglDestroySurface(display, surface)
    }

    companion object : GLContextProvider<EGLContext> {
        override fun <R> restorePrevious(block: () -> R): R {
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

        override fun fromCurrent(): EGLContext? {
            val display = eglGetCurrentDisplay()
            val context = eglGetCurrentContext()
            val surface = eglGetCurrentSurface(EGL_DRAW)
            if (display == EGL_NO_DISPLAY || context == EGL_NO_CONTEXT || surface == EGL_NO_SURFACE) {
                return null
            }
            return EGLContext(display, context, surface)
        }

        override fun isCurrent(): Boolean {
            val display = eglGetCurrentDisplay()
            val context = eglGetCurrentContext()
            val surface = eglGetCurrentSurface(EGL_DRAW)
            return display != EGL_NO_DISPLAY && context != EGL_NO_CONTEXT && surface != EGL_NO_SURFACE
        }

        override fun createOffscreen(parent: EGLContext): EGLContext {
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
            return EGLContext(display, context, surface)
        }
    }
}
