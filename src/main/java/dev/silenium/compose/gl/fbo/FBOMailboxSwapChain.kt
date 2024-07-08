package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FBOMailboxSwapChain(capacity: Int, override val fboCreator: (IntSize) -> FBOPool.FBO) : FBOSwapChain() {
    override var size: IntSize = IntSize.Zero
        private set
    private val displayLock = ReentrantLock()
    private val waitingLock = ReentrantLock()
    private var waitingFBO: FBOPool.FBO? = null
    private var toDisplay: FBOPool.FBO? = null
    private val renderQueue = ArrayBlockingQueue<FBOPool.FBO>(capacity)

    override fun display(block: (FBOPool.FBO) -> Unit) = displayLock.withLock {
        toDisplay?.let(block)
        waitingLock.withLock {
            val waiting = waitingFBO ?: return
            if (waiting.size != size) {
                waiting.destroy()
                waitingFBO = null
                return
            }
            toDisplay = waitingFBO
            waitingFBO = null
        }
    }

    override suspend fun <R> render(block: suspend (FBOPool.FBO) -> R): R? {
        val fbo = renderQueue.poll() ?: return null
        val result = block(fbo)
        if (fbo.size != size) {
            fbo.destroy()
            return null
        }
        displayLock.withLock {
            toDisplay?.let {
                if (it.size != size) it.destroy()
                else renderQueue.offer(it)
            }
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
        this.size = size
        renderQueue.onEach { it.destroy() }.clear()
        renderQueue.fillRenderQueue(fboCreator, size)
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
}
