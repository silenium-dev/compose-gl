package dev.silenium.compose.gl.surface

import dev.silenium.compose.gl.fbo.FBO

interface GLDisplayScope {
    val fbo: FBO
}

internal class GLDisplayScopeImpl(
    override val fbo: FBO
) : GLDisplayScope
