#include <stdbool.h>
#include <GLES2/gl2.h>
#include "glUtil.h"

#define TAG "myDemo-jni" // 这个是自定义的LOG的标识
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型

int mPboIndex;
int mPboNewIndex;
int mPboSize;
bool mInitRecord = true;
GLuint mPboIds[2];
const char *glVersion;

EGLSurface mEGLSurface;
EGLContext mEGLContext;
EGLDisplay mEGLDisplay;

bool checkEglError(char *msg) {
    int error;
    if ((error = eglGetError()) != EGL_SUCCESS) {
        LOGI("%s%s%d", msg, ": EGL error: 0x", error);
        return false;
    }
    return true;
}

bool setupEGL(int width, int height) {
    EGLConfig config;
    EGLint numConfigs;
    bool result = true;
    const EGLint attribs[] = {
            EGL_SURFACE_TYPE, EGL_PBUFFER_BIT,
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, 8,
            EGL_STENCIL_SIZE, 0,
            EGL_NONE
    };

    const EGLint pbuf_attribs[] = {
            EGL_WIDTH, width,
            EGL_HEIGHT, height,
            EGL_NONE};


    const EGLint mEGLContextAttribs[] = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE};
    if ((mEGLDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY)) == EGL_NO_DISPLAY) {
        LOGI("Tex eglGetDisplay() returned error %d", eglGetError());
        result = false;
    } else {
        bool ret = eglInitialize(mEGLDisplay, 0, 0);
        if (!ret) {
            LOGI("init mEGLDisplay fail");
            result = false;
        }
    }

    if (!eglChooseConfig(mEGLDisplay, attribs, &config, 1, &numConfigs)) {
        LOGI("eglChooseConfig() returned error %d", eglGetError());
        result = false;
    }

    if (!(mEGLContext = eglCreateContext(mEGLDisplay, config, 0, mEGLContextAttribs))) {
        LOGI("Tex eglCreateContext() returned error %d", eglGetError());
    }

    if (!(mEGLSurface = eglCreatePbufferSurface(mEGLDisplay, config, pbuf_attribs))) {
        LOGI("Tex eglCreatePbufferSurface() returned error %d", eglGetError());
        result = false;
    }
    LOGI("About to make current. Display %p surface %p mEGLContext %p", mEGLDisplay, mEGLSurface,
         mEGLContext);
    if (!eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
        LOGI("Tex eglMakeCurrent() returned error %d", eglGetError());
        result = false;
    }

    if (result && strcmp(glVersion, "OpenGL ES 3.0") >= 0) {
        initPBO(width, height);
    } else {
        LOGI("%s", "can't make pbo");
    }

    return result;
}


void unbindPixelBuffer() {
    glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
    mPboIndex = (mPboIndex + 1) % 2;
    mPboNewIndex = (mPboNewIndex + 1) % 2;
}


void initPBO(int width, int height) {
    glVersion = (const char *) glGetString(GL_VERSION);
    LOGI("opengl version:%s", glVersion);

    mPboIndex = 0;
    mPboNewIndex = 1;
    mInitRecord = true;
    mPboSize = width * height * 4;
    LOGI("picture width,height:%d,%d", width, height);

    glGenBuffers(2, mPboIds);
    if (mPboIds[0] == 0 || mPboIds[1] == 0) {
        LOGI("%s", "generate pbo fail");
        return;
    }
    //绑定到第一个PBO
    glBindBuffer(GL_PIXEL_PACK_BUFFER, mPboIds[0]);
    //设置内存大小
    glBufferData(GL_PIXEL_PACK_BUFFER, mPboSize, NULL, GL_STATIC_READ);

    //绑定到第而个PBO
    glBindBuffer(GL_PIXEL_PACK_BUFFER, mPboIds[1]);
    //设置内存大小
    glBufferData(GL_PIXEL_PACK_BUFFER, mPboSize, NULL, GL_STATIC_READ);

    //解除绑定PBO
    glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
}

unsigned char *readDataFromGPU(int width, int height) {
    if (strcmp(glVersion, "OpenGL ES 3.0") >= 0) {
        LOGI("opengl version:%s", glVersion);
        glBindBuffer(GL_PIXEL_PACK_BUFFER, mPboIds[mPboIndex]);

        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, 0);

        if (mInitRecord) {//第一帧没有数据跳出
            unbindPixelBuffer();
            mInitRecord = false;
            return NULL;
        }

        glBindBuffer(GL_PIXEL_PACK_BUFFER, mPboIds[mPboNewIndex]);


        //glMapBufferRange会等待DMA传输完成，所以需要交替使用pbo,这边获取的是上一帧的内容
        unsigned char *byteBuffer = (unsigned char *) glMapBufferRange(GL_PIXEL_PACK_BUFFER, 0,
                                                                       mPboSize,
                                                                       GL_MAP_READ_BIT);
        if (byteBuffer == NULL) {
            LOGI("%s", "map buffer fail");
        }
        LOGI("pixel:%s", byteBuffer);
        //memcpy(data, byteBuffer, mPboSize);
        glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
        unbindPixelBuffer();
        return byteBuffer;
    } else {
        unsigned char data[width * height * 4];
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, data);
        return data;
    }
}

void destroyPixelBuffers() {
    if (mPboIds != NULL) {
        glDeleteBuffers(2, mPboIds);
    }
}

void deleteEGL() {
    if (mEGLDisplay != EGL_NO_DISPLAY) {
        eglDestroySurface(mEGLDisplay, mEGLSurface);
        eglDestroyContext(mEGLDisplay, mEGLContext);
        eglReleaseThread();
        eglTerminate(mEGLDisplay);
    }
    mEGLDisplay = EGL_NO_DISPLAY;
    mEGLContext = EGL_NO_CONTEXT;
    mEGLSurface = EGL_NO_SURFACE;
}