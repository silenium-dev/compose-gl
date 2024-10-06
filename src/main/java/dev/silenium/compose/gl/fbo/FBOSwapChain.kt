package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import java.util.concurrent.ArrayBlockingQueue

abstract class FBOSwapChain {
    /**
     * Current size of the swap chain
     */
    abstract val size: IntSize

    /**
     * The function that creates the FBOs
     */
    protected abstract val fboCreator: (IntSize) -> FBO

    /**
     * Display the current FBO
     * @param block The block to run with the current FBO for display
     * @return The result of the block
     */
    abstract fun <R> display(block: (FBO) -> R): R?

    /**
     * Render a frame
     * @param block The block to run with the current FBO for rendering
     * @return The result of the block
     */
    abstract suspend fun <R> render(block: suspend (FBO) -> R): R?
    abstract fun resize(size: IntSize)
    abstract fun destroyFBOs()
}

fun ArrayBlockingQueue<FBO>.fillRenderQueue(fboCreator: (IntSize) -> FBO, size: IntSize) {
    while (remainingCapacity() > 0) {
        offer(fboCreator(size))
    }
}
