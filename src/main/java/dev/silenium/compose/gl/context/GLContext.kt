package dev.silenium.compose.gl.context

import org.lwjgl.opengl.GLCapabilities

interface GLContext {
    val glCapabilities: GLCapabilities
    fun makeCurrent()
    fun unbindCurrent()
    fun destroy()
}

interface GLContextProvider<C: GLContext> {
    fun <R> restorePrevious(block: () -> R): R
    fun fromCurrent(): C?
    fun isCurrent(): Boolean
    fun createOffscreen(parent: C): C
}
