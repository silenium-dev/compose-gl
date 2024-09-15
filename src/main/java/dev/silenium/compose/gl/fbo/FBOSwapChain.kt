package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import java.util.concurrent.ArrayBlockingQueue

abstract class FBOSwapChain {
    abstract val size: IntSize
    protected abstract val fboCreator: (IntSize) -> FBO
    abstract fun display(block: (FBO) -> Unit)
    abstract suspend fun <R> render(block: suspend (FBO) -> R): R?
    abstract fun resize(size: IntSize)
    abstract fun destroyFBOs()
}

fun ArrayBlockingQueue<FBO>.fillRenderQueue(fboCreator: (IntSize) -> FBO, size: IntSize) {
    while (remainingCapacity() > 0) {
        offer(fboCreator(size))
    }
}
