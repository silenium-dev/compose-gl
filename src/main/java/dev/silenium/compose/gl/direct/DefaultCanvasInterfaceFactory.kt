package dev.silenium.compose.gl.direct

import dev.silenium.compose.gl.graphicsApi
import org.jetbrains.skiko.GraphicsApi
import java.awt.Window

object DefaultCanvasInterfaceFactory : CanvasInterfaceFactory<CanvasInterface> {
    override fun create(window: Window): CanvasInterface {
        val factory = apiFactories[window.graphicsApi()]
        factory ?: throw UnsupportedOperationException("Unsupported graphics api: ${window.graphicsApi()}")
        return factory.create(window)
    }

    private val apiFactories = mapOf(
        GraphicsApi.OPENGL to GLCanvasInterface,
        GraphicsApi.DIRECT3D to D3DCanvasInterface,
    )
}
