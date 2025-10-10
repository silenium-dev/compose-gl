package dev.silenium.compose.gl;

import org.jetbrains.skiko.SkiaLayer;

class SkikoCompat {
    public static Object getRedrawer(SkiaLayer layer) {
        return layer.getRedrawer$skiko();
    }
}
