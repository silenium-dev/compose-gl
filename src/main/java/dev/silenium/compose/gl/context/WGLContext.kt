package dev.silenium.compose.gl.context

import dev.silenium.libs.jni.NativeLoader
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.opengl.WGL
import org.lwjgl.opengl.WGLCapabilities
import java.util.concurrent.ConcurrentHashMap

data class WGLContext(
    val deviceContext: Long,
    val renderingContext: Long,
) : GLContext<WGLContext> {
    @Transient
    override val provider: GLContextProvider<WGLContext> = Companion

    @Transient
    lateinit var wglCapabilities: WGLCapabilities

    @Transient
    override lateinit var glCapabilities: GLCapabilities

    override fun unbindCurrent() {
        WGL.wglMakeCurrent(0L, 0L)
    }

    override fun makeCurrent() {
        if (provider.fromCurrent() != this) {
            check(WGL.wglMakeCurrent(deviceContext, renderingContext)) {
                "Failed to make context current"
            }
        }
        val (wglCap, glCap) = contextCapabilities.compute(this) { key, value ->
            value?.let {
                it.copy(refCount = it.refCount + 1)
            } ?: restorePrevious {
                ContextCapabilities(GL.createCapabilitiesWGL(), GL.createCapabilities(), 1)
            }
        }!!
        wglCapabilities = wglCap
        glCapabilities = glCap
    }

    override fun destroy() {
        contextCapabilities.compute(this) { key, value ->
            if (value == null) {
                WGL.wglMakeCurrent(0L, 0L)
                WGL.wglDeleteContext(key.renderingContext)
                return@compute null
            }
            val refCount = value.refCount - 1
            if (refCount == 0) {
                WGL.wglMakeCurrent(0L, 0L)
                WGL.wglDeleteContext(key.renderingContext)
                return@compute null
            }
            value.copy(refCount = refCount)
        }
    }

    override fun deriveOffscreenContext() = provider.createOffscreen(this)

    companion object : GLContextProvider<WGLContext> {
        private data class ContextCapabilities(
            val egl: WGLCapabilities,
            val gl: GLCapabilities,
            val refCount: Int,
        )

        private val contextCapabilities = ConcurrentHashMap<WGLContext, ContextCapabilities>()

        init {
            NativeLoader.loadLibraryFromClasspath("compose-gl").getOrThrow()
        }

        override fun <R> restorePrevious(block: () -> R): R {
            val displayContext = WGL.wglGetCurrentDC()
            val renderingContext = WGL.wglGetCurrentContext()
            return block().also {
                WGL.wglMakeCurrent(displayContext, renderingContext)
            }
        }

        override fun fromCurrent(): WGLContext? {
            val deviceContext = WGL.wglGetCurrentDC()
            val renderingContext = WGL.wglGetCurrentContext()
            return if (deviceContext != 0L && renderingContext != 0L) {
                WGLContext(deviceContext, renderingContext)
            } else {
                null
            }
        }

        override fun isCurrent(): Boolean {
            val deviceContext = WGL.wglGetCurrentDC()
            val renderingContext = WGL.wglGetCurrentContext()
            return deviceContext != 0L && renderingContext != 0L
        }

        override fun createOffscreen(parent: WGLContext): WGLContext {
            val deviceContext = parent.deviceContext
            val renderingContext = WGL.wglCreateContext(deviceContext)
            check(WGL.wglShareLists(parent.renderingContext, renderingContext)) {
                "Failed to share context lists"
            }
            return WGLContext(deviceContext, renderingContext)
        }
    }
}

private external fun wglCreateContext(deviceContext: Long): Long
