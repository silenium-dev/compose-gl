package dev.silenium.compose.gl.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.LocalWindow
import dev.silenium.compose.gl.directContext
import dev.silenium.compose.gl.findSkiaLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun GLCanvas(
    wrapperFactory: CanvasDriverFactory<*> = DefaultCanvasDriverFactory,
    modifier: Modifier = Modifier,
    onDispose: () -> Unit = {},
    onResize: GLDrawScope.(old: IntSize?, new: IntSize) -> Unit = { _, _ -> },
    block: GLDrawScope.() -> Unit,
) {
    val window = LocalWindow.current ?: throw IllegalStateException("No window")
    val wrapper = remember { wrapperFactory.create(window) }
    LaunchedEffect(window) {
        withContext(Dispatchers.IO) {
            while (isActive) {
                window.findSkiaLayer()?.directContext()?.let {
                    wrapper.setup(it)
                    return@withContext
                }
            }
        }
    }
    DisposableEffect(window) {
        onDispose {
            wrapper.dispose(onDispose)
        }
    }
    Canvas(modifier) {
        drawContext.canvas.nativeCanvas
        wrapper.render(this, onResize) {
            drawGL { block() }
        }
        wrapper.display(this)
    }
}
