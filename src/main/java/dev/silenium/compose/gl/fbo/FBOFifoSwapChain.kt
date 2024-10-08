package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class FBOFifoSwapChain(capacity: Int, override val fboCreator: (IntSize) -> FBO) : FBOSwapChain() {
    override var size: IntSize = IntSize.Zero
        private set
    private val displayLock = ReentrantLock()
    private var current: FBO? = null
    private val renderQueue = ArrayBlockingQueue<FBO>(capacity)
    private val displayQueue = ArrayBlockingQueue<FBO>(capacity)

    override fun <R> display(block: (FBO) -> R) = displayLock.withLock {
        val result = current?.let(block)
        if (!displayQueue.isEmpty()) {
            current?.let {
                if (it.size != size) it.destroy()
                else renderQueue.offer(it)
            }
            current = displayQueue.poll()
        }
        return@withLock result
    }

    override fun <R> render(block: (FBO) -> R): R? {
        val fbo = renderQueue.poll() ?: return null
        if (fbo.size != size) {
            fbo.destroy()
            return null
        }
        try {
            val result = block(fbo)
            displayQueue.offer(fbo)
            return result
        } catch (e: Throwable) {
            renderQueue.add(fbo)
            throw e
        }
    }

    override fun resize(size: IntSize) {
        this.size = size
        renderQueue.onEach { it.destroy() }.clear()
        displayQueue.onEach { it.destroy() }.clear()
        renderQueue.fillRenderQueue(fboCreator, size)
    }

    override fun destroyFBOs() {
        renderQueue.onEach { it.destroy() }.clear()
        displayQueue.onEach { it.destroy() }.clear()
        displayLock.withLock {
            current?.destroy()
            current = null
        }
    }
}
