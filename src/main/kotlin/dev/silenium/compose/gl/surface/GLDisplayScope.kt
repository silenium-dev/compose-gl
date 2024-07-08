package dev.silenium.compose.gl.surface

import dev.silenium.compose.gl.FBOPool

interface GLDisplayScope {
    val fbo: FBOPool.FBO
}

internal class GLDisplayScopeImpl(
    override val fbo: FBOPool.FBO
) : GLDisplayScope
