package dev.silenium.compose.gl.canvas

import androidx.compose.runtime.staticCompositionLocalOf
import dev.silenium.compose.gl.findSkiaLayer
import dev.silenium.compose.gl.graphicsApi
import org.jetbrains.skiko.GraphicsApi
import java.awt.Window
import kotlin.collections.get

object DefaultCanvasDriverFactory : CanvasDriverFactory<CanvasDriver> {
    override fun create(window: Window): CanvasDriver {
        val factory = apiFactories[window.findSkiaLayer()?.graphicsApi()]
        factory ?: throw UnsupportedOperationException(
            "Unsupported graphics api: ${
                window.findSkiaLayer()?.graphicsApi()
            }"
        )
        return factory.create(window)
    }

    private val apiFactories = mapOf(
        GraphicsApi.OPENGL to GLCanvasDriver,
        GraphicsApi.DIRECT3D to D3DCanvasDriver,
    )
}
