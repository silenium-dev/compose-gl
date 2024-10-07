package dev.silenium.compose.gl.util

import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

abstract class DoubleDestructionProtection<ID> {
    abstract val id: ID

    private val destroyed = AtomicBoolean(false)
    private var destructionPoint: Throwable? = null
    fun destroy() {
        if (destroyed.compareAndSet(false, true)) {
            destroyInternal()
            destructionPoint = Exception()
        } else {
            logger.trace(
                "{} {} was already destroyed at: {}",
                javaClass.simpleName,
                id,
                destroyed.get(),
                Exception(),
            )
        }
    }

    protected abstract fun destroyInternal()

    companion object {
        private val logger = LoggerFactory.getLogger(DoubleDestructionProtection::class.java)
    }
}
