package dev.silenium.compose.gl

import androidx.compose.runtime.CompositionLocal
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.NativePointer
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.graphicapi.DirectXOffscreenContext
import java.awt.Window
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.typeOf

@Suppress("UNCHECKED_CAST")
val LocalWindow: CompositionLocal<Window?> by lazy {
    val clazz = Class.forName("androidx.compose.ui.window.LocalWindowKt")
    val method = clazz.getMethod("getLocalWindow")
    method.invoke(null) as CompositionLocal<Window?>
}

fun Window.directXRedrawer(): Any? {
    return contextHandler().let {
        val getter = it::class.memberProperties.first { it.name == "directXRedrawer" }
        getter.isAccessible = true
        getter.call(it)
    }
}

fun Window.directX12Device(): NativePointer? {
    return directXRedrawer()?.let {
        val getter = it::class.memberProperties.first { it.name == "device" && it.returnType == typeOf<Long>() }
        getter.isAccessible = true
        getter.call(it) as Long?
    }
}

fun Window.directContext(): DirectContext? {
    val surface = contextHandler().let {
        val getter = it::class.java.superclass.superclass.getDeclaredMethod("getSurface")
        getter.isAccessible = true
        getter.invoke(it) as? Surface
    }
    return surface?.recordingContext
}

fun Window.graphicsApi(): GraphicsApi {
    return mediator().let {
        val getter = it::class.memberProperties.first { it.name == "renderApi" && it.returnType == typeOf<GraphicsApi>() }
        getter.isAccessible = true
        getter.call(it) as GraphicsApi
    }
}

fun Window.mediator(): Any {
    val composePanel = this.getFieldValue("composePanel")!!
    val composeContainer = composePanel.getFieldValue("_composeContainer")!!
    return composeContainer.getFieldValue("mediator")!!
}

fun Window.contextHandler(): Any {
    val contentComponent = mediator().let {
        val getter = it::class.java.getMethod("getContentComponent")
        getter.invoke(it) as SkiaLayer
    }
    val redrawer = contentComponent.let {
        val getter = it::class.java.getMethod("getRedrawer${'$'}skiko")
        getter.invoke(it)
    }
    return redrawer?.getFieldValue("contextHandler")!!
}

private fun Any.getFieldValue(fieldName: String): Any? {
    val field = this::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    return field.get(this)
}
