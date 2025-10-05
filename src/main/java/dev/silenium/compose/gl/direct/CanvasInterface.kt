package dev.silenium.compose.gl.direct

import androidx.compose.ui.graphics.drawscope.DrawScope
import dev.silenium.compose.gl.fbo.FBO
import org.jetbrains.skia.DirectContext
import org.lwjgl.opengl.GL11.glFlush
import java.awt.Window

interface CanvasInterface {
    fun setup(directContext: DirectContext)
    fun render(scope: DrawScope, block: GLDrawScope.() -> Unit)
    fun display(scope: DrawScope)
    fun dispose()
}

fun interface CanvasInterfaceFactory<T : CanvasInterface> {
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
