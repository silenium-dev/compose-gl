package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import java.util.concurrent.ArrayBlockingQueue

abstract class FBOSwapChain {
    abstract val size: IntSize
    protected abstract val fboCreator: (IntSize) -> FBOPool.FBO
    abstract fun display(block: (FBOPool.FBO) -> Unit)
    abstract suspend fun <R> render(block: suspend (FBOPool.FBO) -> R): R?
    abstract fun resize(size: IntSize)
    abstract fun destroyFBOs()
}

fun ArrayBlockingQueue<FBOPool.FBO>.fillRenderQueue(fboCreator: (IntSize) -> FBOPool.FBO, size: IntSize) {
    while (remainingCapacity() > 0) {
        offer(fboCreator(size))
    }
}
