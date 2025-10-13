package dev.silenium.compose.gl.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import dev.silenium.compose.gl.LocalWindow
import dev.silenium.compose.gl.directContext
import dev.silenium.compose.gl.findSkiaLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun GLCanvas(
    state: GLCanvasState = rememberGLCanvasState(),
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
        state.invalidations.let {
            val t1 = System.nanoTime()
            val delta = state.lastFrame?.let { (it - t1).nanoseconds } ?: Duration.ZERO
            wrapper.render(this, userResizeHandler = { old, new ->
                GLDrawScopeImpl(this, delta).onResize(old, new)
            }) {
                drawGL { GLDrawScopeImpl(this, delta).block() }
            }
            val t2 = System.nanoTime()
            state.onRender(t2, (t2 - t1).nanoseconds)
            val t3 = System.nanoTime()
            wrapper.display(this)
            val t4 = System.nanoTime()
            state.onDisplay(t4, (t4 - t3).nanoseconds)
        }
    }
}
