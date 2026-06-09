package dev.silenium.compose.gl.canvas

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import org.jetbrains.skia.DirectContext

interface CanvasDriver {
    fun setup(directContext: DirectContext)
    fun render(
        scope: DrawScope,
        userResizeHandler: FBOScope.(old: IntSize?, new: IntSize) -> Unit = { _, _ -> },
        block: FBOScope.() -> Unit,
    )

    fun display(scope: DrawScope)
    fun dispose(userDisposeHandler: () -> Unit)
}
