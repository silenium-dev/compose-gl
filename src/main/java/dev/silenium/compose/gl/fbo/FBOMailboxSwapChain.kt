package dev.silenium.compose.gl.fbo

import androidx.compose.ui.unit.IntSize
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class FBOMailboxSwapChain(private val capacity: Int, override val fboCreator: (IntSize) -> FBO) : FBOSwapChain() {
    override var size: IntSize = IntSize.Zero
        private set
    private val stateLock = ReentrantReadWriteLock()
    private var fbos: Array<FBO>? = null
    private var current: Int = -1

    override fun <R> display(block: (FBO) -> R): R? = stateLock.read stateLock@{
        if (current == -1) return@stateLock null
        val fbo = fbos?.get(current) ?: return@stateLock null
        val result = block(fbo)
        return@stateLock result
    }

    override fun <R> render(block: (FBO) -> R): R? = stateLock.read {
        val fbos = fbos ?: return null
        val next = (current + 1) % fbos.size
        if (fbos[next].size != size) {
            fbos[next].destroy()
            fbos[next] = fboCreator(size)
        }
        try {
            val result = block(fbos[next])
            current = next
            return result
        } catch (e: Throwable) {
            throw e
        }
    }

    override fun resize(size: IntSize): Unit = stateLock.write {
        this.size = size
        fbos?.forEach(FBO::destroy)
        fbos = Array(capacity) { fboCreator(size) }
    }

    override fun destroyFBOs(): Unit = stateLock.write {
        fbos?.forEach(FBO::destroy)
    }
}
