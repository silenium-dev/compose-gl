#define EGL_EGL_PROTOTYPES 0

#include <jni.h>
#include <iostream>
#include <dlfcn.h>
#include <EGL/egl.h>

extern "C" {
JNIEXPORT jstring JNICALL
Java_dev_silenium_compose_gl_EGL_stringFromJNI(JNIEnv *env, jobject thiz) {
    std::cout << "Hello from EGL" << std::endl;
    return env->NewStringUTF("Hello from EGL");
}

JNIEXPORT void JNICALL
Java_dev_silenium_compose_gl_EGL_debugContext(JNIEnv *env, jobject thiz) {
    const auto egl = dlopen("libEGL.so", RTLD_LAZY);
    const auto eglGetCurrentDisplay = (PFNEGLGETCURRENTDISPLAYPROC) dlsym(egl, "eglGetCurrentDisplay");
    const auto eglGetCurrentContext = (PFNEGLGETCURRENTCONTEXTPROC) dlsym(egl, "eglGetCurrentContext");
    const auto eglGetCurrentSurface = (PFNEGLGETCURRENTSURFACEPROC) dlsym(egl, "eglGetCurrentSurface");
    const auto dpy = eglGetCurrentDisplay();
    const auto ctx = eglGetCurrentContext();
    const auto surface = eglGetCurrentSurface(EGL_DRAW);
    printf("current display: %p\n", dpy);
    printf("current context: %p\n", ctx);
    printf("current surface: %p\n", surface);
    fflush(stdout);
    dlclose(egl);
}
}
