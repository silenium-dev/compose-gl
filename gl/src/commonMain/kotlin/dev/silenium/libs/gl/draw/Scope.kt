package dev.silenium.libs.gl.draw

import dev.silenium.libs.gl.GLProcAddressProvider
import dev.silenium.libs.gl.GLProvider
import dev.silenium.libs.gl.fbo.FBOScope
import kotlin.time.Duration


interface GLDrawScope : FBOScope, GLProcAddressProvider {
    val deltaTime: Duration
}

data class GLDrawScopeImpl(
    val fboScope: FBOScope,
    val procAddressScope: GLProcAddressProvider,
    override val deltaTime: Duration,
) : FBOScope by fboScope, GLDrawScope, GLProcAddressProvider by procAddressScope


fun resetGLFeatures() {
    GLProvider.allGLFeatures.forEach(GLProvider::glDisable)
}
