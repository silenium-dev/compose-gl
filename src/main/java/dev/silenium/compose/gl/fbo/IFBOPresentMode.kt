package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize

interface IFBOPresentMode {
    fun display(block: (FBOPool.FBO) -> Unit)
    suspend fun <R> render(block: suspend (FBOPool.FBO) -> R): R?
    fun resize(size: IntSize)
    fun destroyFBOs()
}
