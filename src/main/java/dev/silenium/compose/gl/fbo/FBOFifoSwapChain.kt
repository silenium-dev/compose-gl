package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

class FBOFifoSwapChain(private val capacity: Int, override val fboCreator: (IntSize) -> FBO) : FBOSwapChain() {
    override var size: IntSize = IntSize.Zero
        private set
    private var fbos = AtomicReference<Array<FBO>?>(null)
    private var current: Int = -1
    private val displayQueue = ConcurrentLinkedQueue<Int>()
    private val renderQueue = ConcurrentLinkedQueue<Int>()

    override fun <R> display(block: (FBO) -> R): R? {
        val next = displayQueue.poll()
        if (next != null && next != -1) {
            renderQueue.add(current)
            current = next
        }
        if (current == -1) return null
        val fbo = fbos.get()?.get(current) ?: return null
        val result = block(fbo)
        return result
    }

    override fun <R> render(block: (FBO) -> R): R? {
        val next = renderQueue.poll() ?: return null
        val nextFbos = fbos.updateAndGet {
            if (it?.get(next)?.size != size) {
                it?.get(next)?.destroy()
                it?.set(next, fboCreator(size))
            }
            it
        }
        try {
            val nextFbo = nextFbos?.get(next) ?: return null
            val result = block(nextFbo)
            current = next
            return result
        } catch (e: Throwable) {
            throw e
        }
    }

    override fun resize(size: IntSize) {
        this.size = size
        renderQueue.clear()
        displayQueue.clear()
        fbos.get()?.forEachIndexed { index, fbo ->
            if (fbo.size != size && index != current) {
                fbo.destroy()
            }
        }
        val currentFbo = fbos.get()?.getOrNull(current)
        fbos.set(Array(capacity) {
            if (it == current) currentFbo ?: fboCreator(size)
            else fboCreator(size)
        }.also {
            renderQueue.addAll(it.indices - current)
        })
    }

    override fun destroyFBOs() {
        renderQueue.clear()
        displayQueue.clear()
        current = -1
        fbos.updateAndGet {
            it?.forEach(FBO::destroy)
            null
        }
    }
}
