package dev.silenium.compose.gl.interop;

import org.jetbrains.skia.BackendTexture;

class Compat {
    static BackendTexture create(long nativePtr) {
        return new BackendTexture(nativePtr);
    }
}
