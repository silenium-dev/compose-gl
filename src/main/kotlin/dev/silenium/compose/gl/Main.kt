package dev.silenium.compose.gl

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.silenium.compose.gl.surface.GLSurfaceView
import org.jetbrains.skia.DirectContext
import org.jetbrains.skiko.SkiaLayer
import org.lwjgl.opengles.GLES30.*
import kotlin.random.Random
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.time.Duration.Companion.milliseconds

@Composable
@Preview
fun App() {
//    var text by remember { mutableStateOf("Hello, World!") }
//
//    var topLeft by remember { mutableStateOf(Offset.Zero) }
//    var size by remember { mutableStateOf(Size.Zero) }
//    var windowSize by remember { mutableStateOf(IntSize.Zero) }
//
//    val window = LocalWindowInfo.current
//    LaunchedEffect(window.containerSize) {
//        windowSize = window.containerSize
//    }
//    val glCapabilitiesCreated = remember { AtomicBoolean(false) }

    MaterialTheme {
        Box(contentAlignment = Alignment.TopStart) {
            GLSurfaceView(modifier = Modifier.size(100.dp)) {
                glClearColor(Random.nextFloat(), Random.nextFloat(), Random.nextFloat(), 1.0f)
                glClear(GL_COLOR_BUFFER_BIT)
                val wait = (1000.0 / 240).milliseconds
                redrawAfter(wait)
                println("Delta time: $deltaTime")
                println("Wait: $wait")
                println("FPS: ${1_000_000.0 / deltaTime.inWholeMicroseconds}")
            }
            Button(onClick = {}) {
                Text("Click me!")
            }
        }
        /* Surface(
             modifier = Modifier
                 .onGloballyPositioned {
                     topLeft = it.boundsInParent().topLeft
                     size = it.boundsInParent().size
                 }
                 .drawWithContent {
                     if (glCapabilitiesCreated.compareAndSet(false, true)) {
                         GL.createCapabilities()
                         val dpy = org.lwjgl.egl.EGL15.eglGetCurrentDisplay()
                         println("dpy: $dpy")
                         EGL.debugContext()
                     }
                     drawContent()
                 }
         ) {
             Column {
                 Text(text)
                 Button(onClick = { text = "Hello, Desktop!" }) {
                     Text("Click me!")
                 }
             }
         }*/
    }
}

val LocalWindow = staticCompositionLocalOf<ComposeWindow> { error("No DirectContext provided") }

fun ComposeWindow.directContext(): DirectContext? {
    val composePanelProp = ComposeWindow::class.memberProperties.first { it.name == "composePanel" }
    composePanelProp.isAccessible = true
    val composePanel = composePanelProp.get(this)!!
//    println("composePanel: $composePanel")
    val composeContainerProp =
        composePanel::class.memberProperties.first { it.name == "_composeContainer" } as KProperty1<Any, Any>
    composeContainerProp.isAccessible = true
    val composeContainer = composeContainerProp.get(composePanel)
//    println("composeContainer: $composeContainer")
    val mediatorProp = composeContainer::class.memberProperties.first { it.name == "mediator" } as KProperty1<Any, Any>
    mediatorProp.isAccessible = true
    val mediator = mediatorProp.get(composeContainer)
//    println("mediator: $mediator")
    val skiaLayerComponentProp =
        mediator::class.memberProperties.first { it.name == "skiaLayerComponent" } as KProperty1<Any, Any>
    skiaLayerComponentProp.isAccessible = true
    val skiaLayerComponent = skiaLayerComponentProp.get(mediator)
//    println("skiaLayerComponent: $skiaLayerComponent")
    val contentComponentProp =
        skiaLayerComponent::class.memberProperties.first { it.name == "contentComponent" } as KProperty1<Any, Any>
    contentComponentProp.isAccessible = true
    val contentComponent = contentComponentProp.get(skiaLayerComponent) as SkiaLayer
//    println("contentComponent: $contentComponent")
    val redrawerProp = contentComponent::class.memberProperties.first { it.name == "redrawer" } as KProperty1<Any, Any>
    redrawerProp.isAccessible = true
    val redrawer = redrawerProp.get(contentComponent)
//    println("redrawer: $redrawer")
    val contextHandlerProp =
        redrawer::class.memberProperties.first { it.name == "contextHandler" } as KProperty1<Any, Any>
    contextHandlerProp.isAccessible = true
    val contextHandler = contextHandlerProp.get(redrawer)
//    println("contextHandler: $contextHandler")
    val contextProp = contextHandler::class.memberProperties.first { it.name == "context" } as KProperty1<Any, Any>
    contextProp.isAccessible = true
    val context = contextProp.get(contextHandler) as DirectContext?
//    println("context: $context")
    return context
}

fun main() = application {
    println(EGL.stringFromJNI())
    Window(onCloseRequest = ::exitApplication) {
        CompositionLocalProvider(LocalWindow provides window) {
            App()
        }
    }
}
