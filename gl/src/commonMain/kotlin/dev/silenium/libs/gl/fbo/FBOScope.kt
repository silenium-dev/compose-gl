package dev.silenium.libs.gl.fbo

import dev.silenium.libs.gl.GLProvider.glFlush
import dev.silenium.libs.gl.draw.resetGLFeatures

interface FBOScope {
    val fbo: FBO
}

data class FBOScopeImpl(override val fbo: FBO) : FBOScope

inline fun <T> FBOScope.drawGL(block: () -> T): T {
    fbo.bind()
    resetGLFeatures()
    try {
        return block()
    } finally {
        fbo.unbind()
        glFlush()
    }
}
