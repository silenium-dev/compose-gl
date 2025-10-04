//
// Created by silenium-dev on 2024-09-15.
//

#include <Windows.h>
#include <wingdi.h>
#include <gl/GL.h>
#include <jni.h>

extern "C" {
JNIEXPORT jlong JNICALL Java_dev_silenium_compose_gl_context_WGLContextKt_wglCreateContext(JNIEnv *env, jobject thiz, jlong _hdc) {
    const auto hdc = reinterpret_cast<HDC>(_hdc);
    const auto ctx = wglCreateContext(hdc);
    return reinterpret_cast<jlong>(ctx);
}
}
