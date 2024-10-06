package dev.silenium.compose.gl.surface

import dev.silenium.compose.gl.fbo.FBO
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface GLDrawScope {
    /**
     * Current FBO.
     */
    val fbo: FBO

    /**
     * Time since the last frame.
     */
    val deltaTime: Duration

    /**
     * Redraw after the given duration.
     */
    fun redrawAfter(duration: Duration)
}

internal class GLDrawScopeImpl(
    override val fbo: FBO,
    override val deltaTime: Duration,
) : GLDrawScope {
    internal var redrawAfter = (1000 / 60).milliseconds
        private set

    override fun redrawAfter(duration: Duration) {
        redrawAfter = duration
    }
}
