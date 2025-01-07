package dev.silenium.compose.gl.context

import dev.silenium.libs.jni.NativePlatform
import dev.silenium.libs.jni.Platform
import org.lwjgl.system.Configuration
import org.slf4j.LoggerFactory

object GLContextProviderFactory {
    init {
        Configuration.OPENGL_CONTEXT_API.set("native")
        Configuration.OPENGLES_CONTEXT_API.set("native")
    }

    private val log = LoggerFactory.getLogger(GLContextProviderFactory::class.java)

    private val override by lazy {
        System.getProperty("compose.gl.context.provider")
            ?.takeIf { it.isNotBlank() }
            ?.let { enumValueOf<GLContextProviderType>(it) }
    }

    /**
     * The detected GL context provider.
     * @see detect
     */
    val detected: GLContextProvider<*> by lazy(::detect)

    private enum class GLContextProviderType(val provider: GLContextProvider<*>) {
        EGL(EGLContext),
        GLX(GLXContext);
    }

    private val osOrder = mapOf(
        Platform.OS.LINUX to listOf(GLContextProviderType.EGL, GLContextProviderType.GLX),
    )

    /**
     * Detects the GL context provider.
     *
     * The provider is detected based on the current platform.
     * The detection order is defined by the [osOrder] map.
     *
     * @return The detected GL context provider.
     */
    fun detect(): GLContextProvider<*> {
        override?.let {
            log.info("Using overridden GL context provider: {}", it)
            return it.provider
        }

        val platform = NativePlatform.platform()
        log.debug("Detecting GL context provider for platform: {}", platform)
        val order = osOrder[platform.os]
            ?: throw UnsupportedOperationException("Unsupported platform: ${NativePlatform.os}")
        for (type in order) {
            log.debug("Trying {} context", type)
            if (type.provider.fromCurrent() != null) {
                log.info("Using {} context", type)
                return type.provider
            }
        }
        return osOrder[platform.os]?.firstOrNull()?.provider
            ?: throw UnsupportedOperationException("No GL context provider found")
    }
}
