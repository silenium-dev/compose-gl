//
// Created by silenium-dev on 7/28/24.
//

#include <cstring>
#include <iostream>
#include <jni.h>

#include <GL/glx.h>

#define GLX_CONTEXT_MAJOR_VERSION_ARB       0x2091
#define GLX_CONTEXT_MINOR_VERSION_ARB       0x2092

typedef GLXContext (*glXCreateContextAttribsARBProc)(Display *, GLXFBConfig, GLXContext, Bool, const int *);

static int handleError(Display *display, XErrorEvent *event) {
    std::cerr << "X Error: " << static_cast<int>(event->error_code) << std::endl;
    std::cerr << "  Request: " << static_cast<int>(event->request_code) << std::endl;
    std::cerr << "  Error: " << static_cast<int>(event->error_code) << std::endl;
    char buf[256];
    XGetErrorText(display, event->error_code, buf, sizeof(buf));
    std::cerr << "  Text: " << buf << std::endl;
    return 0;
}

// Helper to check for extension string presence.  Adapted from:
//   http://www.opengl.org/resources/features/OGLextensions/
static bool isExtensionSupported(const char *extList, const char *extension) {
    /* Extension names should not have spaces. */
    const char *where = strchr(extension, ' ');
    if (where || *extension == '\0')
        return false;

    /* It takes a bit of care to be fool-proof about parsing the
       OpenGL extensions string. Don't be fooled by sub-strings,
       etc. */
    for (const char *start = extList;;) {
        where = strstr(start, extension);

        if (!where)
            break;

        const char *terminator = where + strlen(extension);

        if (where == start || *(where - 1) == ' ')
            if (*terminator == ' ' || *terminator == '\0')
                return true;

        start = terminator;
    }

    return false;
}

extern "C" {
JNIEXPORT jlong JNICALL Java_dev_silenium_compose_gl_context_GLXContextKt_getCurrentContextN(JNIEnv *env, jobject thiz) {
    return reinterpret_cast<jlong>(glXGetCurrentContext());
}

JNIEXPORT jlong JNICALL Java_dev_silenium_compose_gl_context_GLXContextKt_getCurrentDisplayN(JNIEnv *env, jobject thiz) {
    return reinterpret_cast<jlong>(glXGetCurrentDisplay());
}

JNIEXPORT jlong JNICALL
Java_dev_silenium_compose_gl_context_GLXContextKt_getCurrentDrawableN(JNIEnv *env, jobject thiz) {
    return static_cast<jlong>(glXGetCurrentDrawable());
}

JNIEXPORT jlongArray JNICALL
Java_dev_silenium_compose_gl_context_GLXContextKt_createContextN(JNIEnv *env, jobject thiz,
                                                             const jlong _display, const jlong share) {
    auto result = env->NewLongArray(3);

    const auto display = reinterpret_cast<Display *>(_display);

    constexpr int attribs[]{
        GLX_RED_SIZE, 8,
        GLX_GREEN_SIZE, 8,
        GLX_BLUE_SIZE, 8,
        GLX_ALPHA_SIZE, 8,
        GLX_DRAWABLE_TYPE, GLX_PIXMAP_BIT,
        GLX_DEPTH_SIZE, 24,
        GLX_STENCIL_SIZE, 8,
        None
    };
    int fbConfigCount{0};
    const auto fbConfig = glXChooseFBConfig(display, DefaultScreen(display), attribs, &fbConfigCount);
    if (fbConfig == nullptr || fbConfigCount <= 0) {
        std::cerr << "Failed to get FBConfig" << std::endl;
        env->DeleteLocalRef(result);
        return nullptr;
    }
    const auto fbc = fbConfig[0];
    XFree(fbConfig);

    int attribs2[]{
        GLX_NONE,
    };
    const auto xPixmap = XCreatePixmap(display, RootWindow(display, DefaultScreen(display)), 16, 16, 24);
    if (xPixmap == None) {
        std::cerr << "Failed to create X Pixmap" << std::endl;
        env->DeleteLocalRef(result);
        return nullptr;
    }
    const auto glxPixmap = glXCreatePixmap(display, fbc, xPixmap, attribs2);
    if (glxPixmap == None) {
        std::cerr << "Failed to create PBuffer" << std::endl;
        env->DeleteLocalRef(result);
        return nullptr;
    }
    const jlong xPixmapLong = *reinterpret_cast<const jlong *>(&xPixmap);
    const jlong glxPixmapLong = *reinterpret_cast<const jlong *>(&glxPixmap);
    env->SetLongArrayRegion(result, 0, 1, &xPixmapLong);
    env->SetLongArrayRegion(result, 1, 1, &glxPixmapLong);

    const auto visual = glXGetVisualFromFBConfig(display, fbc);
    if (visual == nullptr) {
        std::cerr << "Failed to get Visual" << std::endl;
        env->DeleteLocalRef(result);
        return nullptr;
    }

    const char *glxExts = glXQueryExtensionsString(display, DefaultScreen(display));
    auto glXCreateContextAttribsARB = reinterpret_cast<glXCreateContextAttribsARBProc>(
        glXGetProcAddressARB(reinterpret_cast<const GLubyte *>("glXCreateContextAttribsARB"))
    );

    GLXContext ctx{nullptr};
    if (!isExtensionSupported(glxExts, "GLX_ARB_create_context") ||
        !glXCreateContextAttribsARB) {
        std::cout << "glXCreateContextAttribsARB() not found ... using old-style GLX context" << std::endl;
        ctx = glXCreateNewContext(display, fbc, GLX_RGBA_TYPE, reinterpret_cast<GLXContext>(share), True);
    } else {
        int context_attribs[] = {
            GLX_CONTEXT_MAJOR_VERSION_ARB, 3,
            GLX_CONTEXT_MINOR_VERSION_ARB, 0,
            //GLX_CONTEXT_FLAGS_ARB        , GLX_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB,
            None
        };
        ctx = glXCreateContextAttribsARB(display, fbc, reinterpret_cast<GLXContext>(share), True, context_attribs);
    }

    if (ctx == nullptr) {
        std::cerr << "Failed to create Context" << std::endl;
        env->DeleteLocalRef(result);
        return nullptr;
    }
    const auto ctxLong = reinterpret_cast<jlong>(ctx);
    env->SetLongArrayRegion(result, 2, 1, &ctxLong);

    return result;
}

JNIEXPORT void JNICALL
Java_dev_silenium_compose_gl_context_GLXContextKt_destroyPixmapN(JNIEnv *env, jobject thiz,
                                                             const jlong _display, const jlong xPixmap,
                                                             const jlong glxPixmap) {
    const auto display = reinterpret_cast<Display *>(_display);
    const auto xPixmap_ = *reinterpret_cast<const Pixmap *>(&xPixmap);
    const auto glxPixmap_ = *reinterpret_cast<const GLXPixmap *>(&glxPixmap);
    glXDestroyPixmap(display, glxPixmap_);
    if (xPixmap_ != None) {
        XFreePixmap(display, xPixmap_);
    }
}
}
