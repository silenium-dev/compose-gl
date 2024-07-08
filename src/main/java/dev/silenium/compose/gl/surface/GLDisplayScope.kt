package dev.silenium.compose.gl.surface

interface GLDisplayScope {
    val fbo: FBOPool.FBO
}

internal class GLDisplayScopeImpl(
    override val fbo: FBOPool.FBO
) : GLDisplayScope
