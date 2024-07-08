package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FBOFifo(capacity: Int, private val fboCreator: (IntSize) -> FBOPool.FBO) : IFBOPresentMode {
    private val displayLock = ReentrantLock()
    private var toDisplay: FBOPool.FBO? = null
    private val renderQueue = ArrayBlockingQueue<FBOPool.FBO>(capacity)
    private val displayQueue = ArrayBlockingQueue<FBOPool.FBO>(capacity)

    override fun display(block: (FBOPool.FBO) -> Unit) = displayLock.withLock {
        toDisplay?.let(block)
        if (!displayQueue.isEmpty()) {
            toDisplay?.let(renderQueue::offer)
            toDisplay = displayQueue.poll()
        }
    }

    override suspend fun <R> render(block: suspend (FBOPool.FBO) -> R): R? {
        val fbo = renderQueue.poll() ?: return null
        val result = block(fbo)
        displayQueue.offer(fbo)
        return result
    }

    override fun resize(size: IntSize) {
        destroyFBOs()
        fillRenderQueue { fboCreator(size) }
    }

    override fun destroyFBOs() {
        renderQueue.onEach { it.destroy() }.clear()
        displayQueue.onEach { it.destroy() }.clear()
        displayLock.withLock {
            toDisplay?.destroy()
            toDisplay = null
        }
    }

    private fun fillRenderQueue(generator: () -> FBOPool.FBO) {
        while (renderQueue.remainingCapacity() > 0) {
            renderQueue.offer(generator())
        }
    }
}
