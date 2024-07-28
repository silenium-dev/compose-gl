package dev.silenium.compose.gl.context

import dev.silenium.libs.jni.NativeLoader
import org.lwjgl.egl.EGL15.*
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GLCapabilities
import org.lwjgl.opengl.GLX
import org.lwjgl.opengl.GLXCapabilities

data class GLXContext(
    val display: Long,
    val drawable: Long,
    val context: Long,
    val xDrawable: Long? = null,
) : GLContext {
    lateinit var glxCapabilities: GLXCapabilities
        private set
    override lateinit var glCapabilities: GLCapabilities
        private set

    init {
        restorePrevious {
            makeCurrent()
            glxCapabilities = GL.createCapabilitiesGLX(display)
            glCapabilities = GL.createCapabilities()
        }
    }

    override fun makeCurrent() {
        check(GLX.glXMakeCurrent(display, drawable, context)) {
            "Failed to make context current"
        }
    }

    override fun unbindCurrent() {
        check(GLX.glXMakeCurrent(display, 0L, 0L)) {
            "Failed to unbind context"
        }
    }

    override fun destroy() {
        unbindCurrent()
        GLX.glXDestroyContext(display, context)
        destroyPixmapN(display, xDrawable ?: 0L, drawable)
    }

    companion object : GLContextProvider<GLXContext> {
        init {
            NativeLoader.loadLibraryFromClasspath("compose-gl")
        }

        override fun <R> restorePrevious(block: () -> R): R {
            val display = getCurrentDisplayN()
            val context = getCurrentContextN()
            val drawable = getCurrentDrawableN()
            if (display == EGL_NO_DISPLAY || context == EGL_NO_CONTEXT || drawable == EGL_NO_SURFACE) {
                return block()
            }
            return block().also {
                GLX.glXMakeCurrent(display, drawable, context)
            }
        }

        override fun fromCurrent(): GLXContext? {
            val display = getCurrentDisplayN()
            val context = getCurrentContextN()
            val drawable = getCurrentDrawableN()
            if (display == 0L || context == 0L || drawable == 0L) {
                return null
            }
            return GLXContext(display, drawable, context)
        }

        override fun isCurrent(): Boolean {
            val display = getCurrentDisplayN()
            val context = getCurrentContextN()
            val drawable = getCurrentDrawableN()
            return display != 0L && context != 0L && drawable != 0L
        }

        override fun createOffscreen(parent: GLXContext): GLXContext {
            val display = parent.display
            val share = parent.context
            val (xPixmap, drawable, context) = createContextN(display, share)
            return GLXContext(display, drawable, context, xPixmap)
        }
    }
}

private external fun getCurrentContextN(): Long
private external fun getCurrentDisplayN(): Long
private external fun getCurrentDrawableN(): Long
private external fun createContextN(display: Long, share: Long): LongArray
private external fun destroyPixmapN(display: Long, xPixmap: Long, pixmap: Long)
