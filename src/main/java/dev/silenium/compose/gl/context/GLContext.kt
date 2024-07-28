package dev.silenium.compose.gl.context

import org.lwjgl.opengl.GLCapabilities

interface GLContext<C: GLContext<C>> {
    val provider: GLContextProvider<C>
    val glCapabilities: GLCapabilities
    fun makeCurrent()
    fun unbindCurrent()
    fun destroy()
    fun deriveOffscreenContext(): C
}

interface GLContextProvider<C: GLContext<C>> {
    fun <R> restorePrevious(block: () -> R): R
    fun fromCurrent(): C?
    fun isCurrent(): Boolean
    fun createOffscreen(parent: C): C
}
