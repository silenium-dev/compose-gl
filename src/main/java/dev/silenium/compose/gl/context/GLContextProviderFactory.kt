package dev.silenium.compose.gl.context

import dev.silenium.libs.jni.NativePlatform
import dev.silenium.libs.jni.Platform
import org.slf4j.LoggerFactory

object GLContextProviderFactory {
    private val log = LoggerFactory.getLogger(GLContextProviderFactory::class.java)

    private val override by lazy {
        System.getProperty("compose.gl.context.provider")
            ?.takeIf { it.isNotBlank() }
            ?.let { enumValueOf<GLContextProviderType>(it) }
    }

    val detected: GLContextProvider<*> by lazy(::detect)

    private enum class GLContextProviderType(val provider: GLContextProvider<*>) {
        EGL(EGLContext),
        GLX(GLXContext);
    }

    private val osOrder = mapOf(
        Platform.OS.LINUX to listOf(GLContextProviderType.EGL, GLContextProviderType.GLX),
    )

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
