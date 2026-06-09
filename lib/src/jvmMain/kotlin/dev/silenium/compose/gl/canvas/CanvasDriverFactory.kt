package dev.silenium.compose.gl.canvas

import java.awt.Window

fun interface CanvasDriverFactory<T : CanvasDriver> {
    fun create(window: Window): T
}
