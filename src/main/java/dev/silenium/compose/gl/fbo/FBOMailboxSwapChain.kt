package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import java.util.concurrent.atomic.AtomicReference

class FBOMailboxSwapChain(private val capacity: Int, override val fboCreator: (IntSize) -> FBO) : FBOSwapChain() {
    override var size: IntSize = IntSize.Zero
        private set
    private var fbos = AtomicReference<Array<FBO>?>(null)
    private var current: Int = -1

    override fun <R> display(block: (FBO) -> R): R? {
        if (current == -1) return null
        val fbo = fbos.get()?.get(current) ?: return null
        val result = block(fbo)
        return result
    }

    override fun <R> render(block: (FBO) -> R): R? {
        val next = (current + 1) % capacity
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
        fbos.get()?.forEachIndexed { index, fbo ->
            if (fbo.size != size && index != current) {
                fbo.destroy()
            }
        }
        val currentFbo = fbos.get()?.getOrNull(current)
        fbos.set(Array(capacity) {
            if (it == current) currentFbo ?: fboCreator(size)
            else fboCreator(size)
        })
    }

    override fun destroyFBOs() {
        current = -1
        fbos.updateAndGet {
            it?.forEach(FBO::destroy)
            null
        }
    }
}
