package dev.silenium.compose.gl.canvas

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize

@Composable
expect fun GLCanvas(
    state: GLCanvasState = rememberGLCanvasState(),
    modifier: Modifier = Modifier,
    onDispose: () -> Unit = {},
    onResize: FBOScope.(old: IntSize?, new: IntSize) -> Unit = { _, _ -> },
    block: GLDrawScope.() -> Unit,
)
