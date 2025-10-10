package dev.silenium.compose.gl.interop;

import org.jetbrains.skia.BackendTexture;

/**
 * Java bridge to access internal Skiko methods
 */
class SkikoCompat {
    static BackendTexture create(long nativePtr) {
        return new BackendTexture(nativePtr);
    }
}
