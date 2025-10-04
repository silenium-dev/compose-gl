package dev.silenium.compose.gl

import androidx.compose.runtime.CompositionLocal
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.SkiaLayer
import java.awt.Window

@Suppress("UNCHECKED_CAST")
val LocalWindow: CompositionLocal<Window?> by lazy {
    val clazz = Class.forName("androidx.compose.ui.window.LocalWindowKt")
    val method = clazz.getMethod("getLocalWindow")
    method.invoke(null) as CompositionLocal<Window?>
}

fun Window.directContext(): DirectContext? {
    fun Any.getFieldValue(fieldName: String): Any? {
        val field = this::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(this)
    }

    val composePanel = this.getFieldValue("composePanel")!!
    val composeContainer = composePanel.getFieldValue("_composeContainer")!!
    val mediator = composeContainer.getFieldValue("mediator")!!
    val contentComponent = mediator.let {
        val getter = it::class.java.getMethod("getContentComponent")
        getter.invoke(it) as SkiaLayer
    }
    val redrawer = contentComponent.let {
        val getter = it::class.java.getMethod("getRedrawer${'$'}skiko")
        getter.invoke(it)
    }
    val contextHandler = redrawer.getFieldValue("contextHandler")!!
    val surface = contextHandler.let {
        val getter = it::class.java.superclass.superclass.getDeclaredMethod("getSurface")
        getter.isAccessible = true
        getter.invoke(it) as? Surface
    }
    return surface?.recordingContext
}
