package dev.silenium.compose.gl.canvas

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.fbo.FBO
import org.jetbrains.skia.DirectContext
import org.lwjgl.opengl.GL11.glFlush
import java.awt.Window

interface CanvasDriver {
    fun setup(directContext: DirectContext)
    fun render(
        scope: DrawScope,
        userResizeHandler: GLDrawScope.(old: IntSize?, new: IntSize) -> Unit = { _, _ -> },
        block: GLDrawScope.() -> Unit,
    )

    fun display(scope: DrawScope)
    fun dispose(userDisposeHandler: () -> Unit)
}

fun interface CanvasDriverFactory<T : CanvasDriver> {
    fun create(window: Window): T
}

data class GLDrawScope(
    val fbo: FBO,
)

internal inline fun <T> GLDrawScope.drawGL(block: () -> T): T {
    fbo.bind()
    try {
        return block()
    } finally {
        fbo.unbind()
        glFlush()
    }
}
