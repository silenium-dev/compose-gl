package dev.silenium.compose.gl

import androidx.compose.runtime.CompositionLocal
import org.jetbrains.skia.DirectContext
import org.jetbrains.skiko.SkiaLayer
import java.awt.Window
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Suppress("UNCHECKED_CAST")
val LocalWindow: CompositionLocal<Window?> by lazy {
    val clazz = Class.forName("androidx.compose.ui.window.LocalWindowKt")
    val method = clazz.getMethod("getLocalWindow")
    method.invoke(null) as CompositionLocal<Window?>
}

@Suppress("UNCHECKED_CAST")
internal fun Window.directContext(): DirectContext? {
    val composePanelProp = this::class.memberProperties.first { it.name == "composePanel" } as KProperty1<Any, Any>
    composePanelProp.isAccessible = true
    val composePanel = composePanelProp.get(this)
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
