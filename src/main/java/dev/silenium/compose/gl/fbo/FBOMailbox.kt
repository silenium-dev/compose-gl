package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FBOMailbox(capacity: Int, private val fboCreator: (IntSize) -> FBOPool.FBO) : IFBOPresentMode {
    private val displayLock = ReentrantLock()
    private val waitingLock = ReentrantLock()
    private var waitingFBO: FBOPool.FBO? = null
    private var toDisplay: FBOPool.FBO? = null
    private val renderQueue = ArrayBlockingQueue<FBOPool.FBO>(capacity)

    override fun display(block: (FBOPool.FBO) -> Unit) = displayLock.withLock {
        toDisplay?.let(block)
        waitingLock.withLock {
            if (waitingFBO == null) return
            toDisplay = waitingFBO
            waitingFBO = null
        }
    }

    override fun <R> render(block: (FBOPool.FBO) -> R): R? {
        val fbo = renderQueue.poll() ?: return null
        val result = block(fbo)
        displayLock.withLock {
            toDisplay?.let(renderQueue::offer)
            toDisplay = fbo
        }
        if (displayLock.tryLock()) {
            toDisplay = fbo
            displayLock.unlock()
        } else waitingLock.withLock {
            waitingFBO = fbo
        }
        return result
    }

    override fun resize(size: IntSize) {
        destroyFBOs()
        fillRenderQueue { fboCreator(size) }
    }

    override fun destroyFBOs() {
        renderQueue.onEach { it.destroy() }.clear()
        displayLock.withLock {
            toDisplay?.destroy()
            toDisplay = null
        }
        waitingLock.withLock {
            waitingFBO?.destroy()
            waitingFBO = null
        }
    }

    private fun fillRenderQueue(generator: () -> FBOPool.FBO) {
        while (renderQueue.remainingCapacity() > 0) {
            renderQueue.offer(generator())
        }
    }
}
