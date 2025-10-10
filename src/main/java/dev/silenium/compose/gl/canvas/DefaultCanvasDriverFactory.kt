package dev.silenium.compose.gl.canvas

import dev.silenium.compose.gl.graphicsApi
import org.jetbrains.skiko.GraphicsApi
import java.awt.Window

object DefaultCanvasDriverFactory : CanvasDriverFactory<CanvasDriver> {
    override fun create(window: Window): CanvasDriver {
        val factory = apiFactories[window.graphicsApi()]
        factory ?: throw UnsupportedOperationException("Unsupported graphics api: ${window.graphicsApi()}")
        return factory.create(window)
    }

    private val apiFactories = mapOf(
        GraphicsApi.OPENGL to GLCanvasDriver,
        GraphicsApi.DIRECT3D to D3DCanvasDriver,
    )
}
