package dev.silenium.compose.gl.interop

import dev.silenium.compose.gl.directX12Device
import dev.silenium.libs.jni.NativeLoader
import org.jetbrains.skia.BackendTexture
import org.jetbrains.skia.impl.NativePointer
import java.awt.Window

object D3DInterop {
    fun createTexture(window: Window, width: Int, height: Int): NativePointer {
        val device = window.directX12Device() ?: throw IllegalStateException("No D3D12 device found")
        return createD3DTextureN(device, width, height)
    }

    fun destroyTexture(texture: NativePointer) {
        destroyD3DTextureN(texture)
    }

    fun exportSharedHandle(window: Window, texture: NativePointer): NativePointer {
        val device = window.directX12Device() ?: throw IllegalStateException("No D3D12 device found")
        return exportSharedHandleN(device, texture)
    }

    fun closeSharedHandle(handle: NativePointer) {
        closeSharedHandleN(handle)
    }

    fun makeBackendTexture(texture: NativePointer): BackendTexture {
        return Compat.create(makeD3DBackendTextureN(texture))
    }

    init {
        NativeLoader.loadLibraryFromClasspath("compose-gl").getOrThrow()
    }
}

private external fun createD3DTextureN(device: NativePointer, width: Int, height: Int): NativePointer
private external fun exportSharedHandleN(device: NativePointer, texture: NativePointer): NativePointer
private external fun makeD3DBackendTextureN(texture: NativePointer): NativePointer
private external fun destroyD3DTextureN(texture: NativePointer)
private external fun closeSharedHandleN(handle: NativePointer)
