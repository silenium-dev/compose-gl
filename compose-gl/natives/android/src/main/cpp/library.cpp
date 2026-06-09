#include <jni.h>
#include <EGL/egl.h>

extern "C" {
    JNIEXPORT jlong JNICALL Java_dev_silenium_compose_gl_natives_android_EGLNative_eglGetProcAddress(JNIEnv* env, jobject thiz, jstring procname) {
        const auto name = env->GetStringUTFChars(procname, nullptr);
        const auto proc = eglGetProcAddress(name);
        env->ReleaseStringUTFChars(procname, name);
        return reinterpret_cast<jlong>(proc);
    }
}
