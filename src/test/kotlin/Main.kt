import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.awaitApplication
import dev.silenium.compose.gl.surface.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.zoomable
import org.jetbrains.skia.Paint
import org.lwjgl.opengl.GL30.*
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
@Preview
fun ApplicationScope.App() {
    MaterialTheme(lightColors()) {
        val state = rememberPagerState { 1000 }
        LaunchedEffect(Unit) {
//            while (isActive) {
//                delay(100.milliseconds)
//                state.animateScrollToPage(Random.nextInt(0..999))
//            }
        }
        VerticalPager(state, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) {
            Content()
        }
    }
}

@Composable
fun ApplicationScope.Content() {
    Box(contentAlignment = Alignment.TopStart, modifier = Modifier.fillMaxSize().background(Color.White)) {
        val state = rememberGLSurfaceState()
        var targetHue by remember { mutableStateOf(0f) }
        val color by animateColorAsState(
            Color.hsl(targetHue, 1f, 0.5f, 0.1f),
            animationSpec = tween(durationMillis = 200, easing = LinearEasing)
        )
        LaunchedEffect(Unit) {
            while (true) {
                targetHue = (targetHue + 10f) % 360f
                delay(200)
            }
        }
        var visible by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(2.seconds)
                visible = !visible
            }
        }
        LaunchedEffect(Unit) {
            while (true) {
                delay(300.milliseconds)
                if (visible) {
                    state.requestUpdate()
                }
            }
        }
        val surfaceView = rememberGLSurface(
            state = state,
            presentMode = GLSurface.PresentMode.MAILBOX,
            fboSizeOverride = FBOSizeOverride(4096, 4096, TransformOrigin.Center),
            swapChainSize = 2,
        ) {
            glClearColor(color.red, color.green, color.blue, color.alpha)
            glClear(GL_COLOR_BUFFER_BIT)
            try {
                Thread.sleep(33, 333)
            } catch (e: InterruptedException) {
                terminate()
                return@rememberGLSurface
            }
            glBegin(GL_QUADS)
            glColor3f(1f, 0f, 0f)
            glVertex2f(-1f, 0f)
            glColor3f(0f, 1f, 0f)
            glVertex2f(0f, -1f)
            glColor3f(0f, 0f, 1f)
            glVertex2f(1f, 0f)
            glColor3f(1f, 1f, 1f)
            glVertex2f(0f, 1f)
            glEnd()

            val wait = (1000.0 / 60).milliseconds
            redrawAfter(null)
        }
        val modifier =  Modifier
            .aspectRatio(1f)
            .zoomable(rememberZoomableState(ZoomSpec(6f)))
            .align(Alignment.Center)
        if (visible) {
            GLSurfaceView(
                surfaceView,
                modifier = modifier,
                paint = Paint().apply {
                    alpha = 128
                }
            )
        } else {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Text("Surface is not visible", style = MaterialTheme.typography.h6, color = MaterialTheme.colors.onBackground, modifier = Modifier.align(Alignment.Center))
            }
        }
        Surface(modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
            Column(modifier = Modifier.padding(4.dp).width(400.dp)) {
                val display by state.displayStatistics.collectAsState()
                Text("Display datapoints: ${display.frameTimes.values.size}")
                Text("Display frame time: ${display.frameTimes.median.inWholeMicroseconds / 1000.0} ms")
                Text("Display frame time (99th): ${display.frameTimes.percentile(0.99).inWholeMicroseconds / 1000.0} ms")
                Text("Display FPS: ${display.fps.median}")
                Text("Display FPS (99th): ${display.fps.percentile(0.99, Stats.Percentile.LOWEST)}")

                val render by state.renderStatistics.collectAsState()
                Text("Render datapoints: ${render.frameTimes.values.size}")
                Text("Render frame time: ${render.frameTimes.median.inWholeMicroseconds / 1000.0} ms")
                Text("Render frame time (99th): ${render.frameTimes.percentile(0.99).inWholeMicroseconds / 1000.0} ms")
                Text("Render FPS: ${render.fps.median} ms")
                Text("Render FPS (99th): ${render.fps.percentile(0.99, Stats.Percentile.LOWEST)}")
            }
        }
        Button(
            onClick = ::exitApplication,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
        ) {
            Text("Exit application")
        }
    }
}

suspend fun main() = awaitApplication {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
