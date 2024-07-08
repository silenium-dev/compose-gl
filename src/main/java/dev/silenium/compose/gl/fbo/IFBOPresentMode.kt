package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize

interface IFBOPresentMode {
    fun display(block: (FBOPool.FBO) -> Unit)
    fun <R> render(block: (FBOPool.FBO) -> R): R?
    fun resize(size: IntSize)
    fun destroyFBOs()
}
