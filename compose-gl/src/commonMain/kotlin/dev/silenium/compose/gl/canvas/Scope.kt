package dev.silenium.compose.gl.canvas

import dev.silenium.libs.gl.GLProvider
import dev.silenium.libs.gl.GLProvider.glFlush
import dev.silenium.libs.gl.fbo.FBO
import kotlin.time.Duration

interface FBOScope {
    val fbo: FBO
}

interface GLProcAddressProvider {
    fun getGlProcAddress(name: String): Long
}

interface GLDrawScope : FBOScope, GLProcAddressProvider {
    val deltaTime: Duration
}

data class GLDrawScopeImpl(
    val fboScope: FBOScope,
    val procAddressScope: GLProcAddressProvider,
    override val deltaTime: Duration,
) : FBOScope by fboScope, GLDrawScope, GLProcAddressProvider by procAddressScope

data class FBOScopeImpl(override val fbo: FBO) : FBOScope

internal inline fun <T> FBOScope.drawGL(block: () -> T): T {
    fbo.bind()
    resetGLFeatures()
    try {
        return block()
    } finally {
        fbo.unbind()
        glFlush()
    }
}

fun resetGLFeatures() {
    GLProvider.allGLFeatures.forEach(GLProvider::glDisable)
}
