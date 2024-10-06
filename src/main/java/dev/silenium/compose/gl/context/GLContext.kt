package dev.silenium.compose.gl.context

import org.lwjgl.opengl.GLCapabilities

/**
 * Represents an OpenGL context.
 * @param C The type of the context.
 */
interface GLContext<C : GLContext<C>> {
    /**
     * The [GLContextProvider] of the context.
     */
    val provider: GLContextProvider<C>

    /**
     * The [GLCapabilities] of the context.
     */
    val glCapabilities: GLCapabilities

    /**
     * Makes the context current.
     * @throws IllegalStateException If the context cannot be made current.
     * @see unbindCurrent
     */
    fun makeCurrent()

    /**
     * Unbinds the current context.
     * @throws IllegalStateException If the context cannot be unbound.
     * @see makeCurrent
     */
    fun unbindCurrent()

    /**
     * Destroys the context.
     * @throws IllegalStateException If the context is currently bound in another thread.
     * @see makeCurrent
     * @see unbindCurrent
     */
    fun destroy()

    /**
     * Derives an offscreen context from the current context.
     * @return The offscreen [GLContext].
     */
    fun deriveOffscreenContext(): C
}

/**
 * Provides a way to create and manage OpenGL contexts.
 */
interface GLContextProvider<C : GLContext<C>> {
    /**
     * Captures the current context, calls the [block] and restores the captured context afterward.
     * This is useful when you need to temporarily switch to another context.
     * @param block The block to call.
     * @return The result of the block.
     */
    fun <R> restorePrevious(block: () -> R): R

    /**
     * Creates a new [GLContext] instance from the current context.
     * @return The new [GLContext] instance.
     */
    fun fromCurrent(): C?

    /**
     * Checks if there is a context current.
     * @return `true` if there is a context current, `false` otherwise.
     */
    fun isCurrent(): Boolean

    /**
     * Creates an offscreen context for the given [parent] context.
     * @param parent The parent [GLContext].
     * @return The offscreen [GLContext].
     */
    fun createOffscreen(parent: C): C
}
