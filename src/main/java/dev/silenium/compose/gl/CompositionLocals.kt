package dev.silenium.compose.gl

import androidx.compose.runtime.CompositionLocal
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.impl.NativePointer
import org.jetbrains.skiko.GraphicsApi
import org.jetbrains.skiko.SkiaLayer
import java.awt.Container
import java.awt.Window
import java.util.*
import javax.swing.JComponent
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.typeOf

@Suppress("UNCHECKED_CAST")
val LocalWindow: CompositionLocal<Window?> by lazy {
    val clazz = Class.forName("androidx.compose.ui.window.LocalWindowKt")
    val method = clazz.getMethod("getLocalWindow")
    method.invoke(null) as CompositionLocal<Window?>
}

fun Window.directX12Device(): NativePointer? {
    return findSkiaLayer()?.let { layer ->
        if (layer.graphicsApi() != GraphicsApi.DIRECT3D) return null
        layer.redrawer().let { redrawer ->
            val getter = redrawer::class.memberProperties.first {
                it.name == "device" && it.returnType == typeOf<Long>()
            }
            getter.isAccessible = true
            getter.call(redrawer) as Long?
        }
    }
}

fun SkiaLayer.directContext(): DirectContext? {
    return contextHandler()?.findProperty<DirectContext?>()
}

inline fun <reified T> Any.findProperty(): T? {
    return findProperty(typeOf<T>()) as T?
}

fun Any.findProperty(type: KType): Any? {
    val supertypes = LinkedList<KClass<*>>()
    supertypes.add(this::class)
    var getter: KProperty1<*, *>? = null
    while (getter == null && supertypes.isNotEmpty()) {
        val klass = supertypes.pop()
        for (prop in klass.memberProperties) {
            if (prop.returnType.isSubtypeOf(type)) {
                getter = prop
                break
            }
        }
        klass.superclasses.let(supertypes::addAll)
    }
    getter?.isAccessible = true
    return getter?.call(this)
}

fun SkiaLayer.graphicsApi(): GraphicsApi {
    return findProperty<GraphicsApi>() ?: GraphicsApi.UNKNOWN
}

fun SkiaLayer.contextHandler(): Any? {
    val propType = Class.forName("org.jetbrains.skiko.context.ContextHandler")
    return redrawer().findProperty(propType.kotlin.createType())
}

fun SkiaLayer.redrawer(): Any = SkikoCompat.getRedrawer(this)

fun Window.findSkiaLayer() = findComponent<SkiaLayer>()

private fun <T : JComponent> findComponent(
    container: Container,
    klass: Class<T>,
): T? {
    val componentSequence = container.components.asSequence()
    return componentSequence
        .filter { klass.isInstance(it) }
        .ifEmpty {
            componentSequence
                .filterIsInstance<Container>()
                .mapNotNull { findComponent(it, klass) }
        }.map { klass.cast(it) }
        .firstOrNull()
}

private inline fun <reified T : JComponent> Container.findComponent() = findComponent(this, T::class.java)
