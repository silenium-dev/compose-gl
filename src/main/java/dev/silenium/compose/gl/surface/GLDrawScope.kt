package dev.silenium.compose.gl.surface

import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.fbo.FBO
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface GLDrawScope {
    val size: IntSize
    val deltaTime: Duration
    fun redrawAfter(duration: Duration)
}

internal class GLDrawScopeImpl(
    private val fbo: FBO,
    override val deltaTime: Duration,
) : GLDrawScope {
    override val size: IntSize by fbo::size
    internal var redrawAfter = (1000 / 60).milliseconds
        private set

    override fun redrawAfter(duration: Duration) {
        redrawAfter = duration
    }
}
