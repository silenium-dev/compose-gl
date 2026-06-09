package dev.silenium.compose.gl.canvas

import dev.silenium.compose.gl.GLProvider
import dev.silenium.compose.gl.GLProvider.glFlush
import dev.silenium.compose.gl.fbo.FBO
import kotlin.time.Duration

interface FBOScope {
    val fbo: FBO
}

interface GLDrawScope : FBOScope {
    val deltaTime: Duration
}

data class GLDrawScopeImpl(
    val fboScope: FBOScope,
    override val deltaTime: Duration,
) : FBOScope by fboScope, GLDrawScope

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
