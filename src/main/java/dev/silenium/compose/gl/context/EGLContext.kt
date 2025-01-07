package dev.silenium.compose.gl.context

import dev.silenium.libs.jni.NativeLoader
import org.lwjgl.egl.EGL
import org.lwjgl.egl.EGL15.*
import org.lwjgl.egl.EGLCapabilities
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.system.MemoryUtil
import java.util.concurrent.ConcurrentHashMap

data class EGLContext(
    val display: Long,
    val context: Long,
    val surface: Long,
) : GLContext<EGLContext> {
    @Transient
    override val provider = Companion

    @Transient
    val eglCapabilities: EGLCapabilities

    @Transient
    override val glCapabilities: GLCapabilities

    init {
        val (eglCap, glCap) = contextCapabilities.compute(this) { key, value ->
            value?.let {
                it.copy(refCount = it.refCount + 1)
            } ?: restorePrevious {
                key.makeCurrent()
                ContextCapabilities(EGL.createDisplayCapabilities(key.display), GL.createCapabilities(), 1)
            }
        }!!
        eglCapabilities = eglCap
        glCapabilities = glCap
    }

    override fun makeCurrent() {
        check(eglMakeCurrent(display, surface, surface, context)) { "Failed to make context current" }
    }

    override fun unbindCurrent() {
        check(eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)) { "Failed to unbind context" }
    }

    override fun destroy() {
        contextCapabilities.compute(this) { _, value ->
            if (value == null) {
                eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
                eglDestroyContext(display, context)
                eglDestroySurface(display, surface)
                return@compute null
            }
            val refCount = value.refCount - 1
            if (refCount == 0) {
                eglMakeCurrent(display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
                eglDestroyContext(display, context)
                eglDestroySurface(display, surface)
                return@compute null
            }
            value.copy(refCount = refCount)
        }
    }

    override fun deriveOffscreenContext() = provider.createOffscreen(this)

    companion object : GLContextProvider<EGLContext> {
        private data class ContextCapabilities(
            val egl: EGLCapabilities,
            val gl: GLCapabilities,
            val refCount: Int,
        )

        private val contextCapabilities = ConcurrentHashMap<EGLContext, ContextCapabilities>()

        init {
            NativeLoader.loadLibraryFromClasspath("compose-gl").getOrThrow()
        }

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
