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
     * @param duration The duration to redraw after. If null, a call to [GLSurfaceState.requestUpdate] is required to redraw.
     */
    fun redrawAfter(duration: Duration?)

    /**
     * Terminate the rendering thread.
     */
    fun terminate()
}

internal class GLDrawScopeImpl(
    override val fbo: FBO,
    override val deltaTime: Duration,
) : GLDrawScope {
    internal var redrawAfter: Duration? = (1000 / 60).milliseconds
        private set
    internal var terminate = false
        private set

    override fun terminate() {
        terminate = true
    }

    override fun redrawAfter(duration: Duration?) {
        redrawAfter = duration
    }
}
