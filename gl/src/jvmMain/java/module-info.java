module dev.silenium.libs.gl {
    requires java.base;
    requires java.desktop;
    requires kotlin.stdlib;
    requires kotlin.reflect;
    requires org.jetbrains.annotations;
    requires kotlinx.coroutines.core;
    requires org.slf4j;
    requires org.lwjgl.glfw;
    requires org.lwjgl.opengl;
    requires org.lwjgl;

    exports dev.silenium.libs.gl;
    exports dev.silenium.libs.gl.fbo;
    exports dev.silenium.libs.gl.objects;
    exports dev.silenium.libs.gl.util;
}
