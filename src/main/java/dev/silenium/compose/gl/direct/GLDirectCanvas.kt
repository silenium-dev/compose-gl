package dev.silenium.compose.gl.direct

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.silenium.compose.gl.LocalWindow
import dev.silenium.compose.gl.directContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun GLCanvas(
    wrapperFactory: CanvasInterfaceFactory<*> = DefaultCanvasInterfaceFactory,
    modifier: Modifier = Modifier,
    block: GLDrawScope.() -> Unit
) {
    val window = LocalWindow.current ?: throw IllegalStateException("No window")
    val wrapper = remember { wrapperFactory.create(window) }
    LaunchedEffect(window) {
        withContext(Dispatchers.IO) {
            while (isActive) {
                window.directContext()?.let {
                    wrapper.setup(it)
                    return@withContext
                }
            }
        }
    }
    DisposableEffect(window) {
        onDispose {
            wrapper.dispose()
        }
    }
    Canvas(modifier) {
        wrapper.render(this) {
            drawGL { block() }
        }
        wrapper.display(this)
    }
}
