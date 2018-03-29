#include "gl_util.h"
#include "jni.h"


#define MY_TAG  "gl_util"
#define loge(format, ...)  __android_log_print(ANDROID_LOG_ERROR, MY_TAG, format, ##__VA_ARGS__)
#define logd(format, ...)  __android_log_print(ANDROID_LOG_DEBUG,  MY_TAG, format, ##__VA_ARGS__)


int mPboIndex;
int mPboNewIndex;
int mPboSize;
bool mInitRecord = true;
GLuint mPboIds[2];
const char *glVersion;

EGLSurface mEGLSurface;
EGLContext mEGLContext;
EGLDisplay mEGLDisplay;

GLuint mTextures[3];
int mWidth;
int mHeight;
int mTextureSize;


void unbindPixelBuffer() {
    glBindBuffer(GL_PIXEL_PACK_BUFFER, 0);
    mPboIndex = (mPboIndex + 1) % 2;
    mPboNewIndex = (mPboNewIndex + 1) % 2;
}

void initTexture() {
    glGenTextures(mTextureSize, mTextures);
    for (int i = 0; i < mTextureSize; i++) {
        glBindTexture(GL_TEXTURE_2D, mTextures[i]);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, mWidth, mHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE,
                     NULL);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }
    glBindTexture(GL_TEXTURE_2D, 0);
}

bool checkEglError(char *msg) {
    int error;
    if ((error = eglGetError()) != EGL_SUCCESS) {
        logd("%s%s%d", msg, ": EGL error: 0x", error);
        return false;
    }
    return true;
}

void initPBO() {
    glVersion = (const char *) glGetString(GL_VERSION);
    logd("opengl version:%s", glVersion);
    //android版本需要低于18，使用gl3stub.c
    gl3stubInit();
//    if (strcmp(glVersion, "OpenGL ES 3.0") < 0) {
//        gl3stubInit();
//    }
    mPboIndex = 0;
    mPboNewIndex = 1;
    mPboSize = mWidth * mHeight * 3 / 2;


    glGenBuffers(2, mPboIds);
    if (mPboIds[0] == 0 || mPboIds[1] == 0) {
        logd("%s", "generate pbo fail");
        return;
    }
    logd("PBO ID :%d,%d", mPboIds[0], mPboIds[1]);
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

bool setupEGL(int width, int height, int textureSize) {
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
        logd("Tex eglGetDisplay() returned error %d", eglGetError());
        result = false;
    } else {
        bool ret = eglInitialize(mEGLDisplay, 0, 0);
        if (!ret) {
            logd("init mEGLDisplay fail");
            result = false;
        }
    }

    if (!eglChooseConfig(mEGLDisplay, attribs, &config, 1, &numConfigs)) {
        logd("eglChooseConfig() returned error %d", eglGetError());
        result = false;
    }

    if (!(mEGLContext = eglCreateContext(mEGLDisplay, config, 0, mEGLContextAttribs))) {
        logd("Tex eglCreateContext() returned error %d", eglGetError());
    }

    if (!(mEGLSurface = eglCreatePbufferSurface(mEGLDisplay, config, pbuf_attribs))) {
        logd("Tex eglCreatePbufferSurface() returned error %d", eglGetError());
        result = false;
    }
    logd("About to make current. Display %p surface %p mEGLContext %p", mEGLDisplay, mEGLSurface,
         mEGLContext);
    if (!eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
        logd("Tex eglMakeCurrent() returned error %d", eglGetError());
        result = false;
    }

    if (result) {
        mWidth = width;
        mHeight = height;
        mTextureSize = textureSize;
        initTexture();
    }

//    if (result && strcmp(glVersion, "OpenGL ES 3.0") >= 0) {

    if (result) {
        initPBO();
    } else {
        logd("%s", "can't make pbo");
    }

    return result;
}


uint8_t *readDataFromGPU(int width, int height) {

    glVersion = (const char *) glGetString(GL_VERSION);

    if (strcmp(glVersion, "OpenGL ES 3.0") >= 0) {
        if (mInitRecord) {
            mWidth = width;
            mHeight = height;
            initPBO();
        }
//        logd("%s", "enter glbind");
        glBindBuffer(GL_PIXEL_PACK_BUFFER, mPboIds[mPboIndex]);

        glReadPixels(0, 0, width, height * 3 / 8, GL_RGBA, GL_UNSIGNED_BYTE, 0);


        if (mInitRecord) {//第一帧没有数据跳出
            unbindPixelBuffer();
            mInitRecord = false;
            return NULL;
        }

        glBindBuffer(GL_PIXEL_PACK_BUFFER, mPboIds[mPboNewIndex]);


        //glMapBufferRange会等待DMA传输完成，所以需要交替使用pbo,这边获取的是上一帧的内容
        uint8_t *byteBuffer = (uint8_t *) glMapBufferRange(GL_PIXEL_PACK_BUFFER, 0,
                                                           mPboSize,
                                                           GL_MAP_READ_BIT);
        if (byteBuffer == NULL) {
            logd("%s", "map buffer fail");
        }
//        logd("pixel:%s", byteBuffer);
        //memcpy(data, byteBuffer, mPboSize);
        glUnmapBuffer(GL_PIXEL_PACK_BUFFER);
        unbindPixelBuffer();
        return byteBuffer;
    } else {
        int size = width * height * 3 / 2;
        uint8_t *data = (uint8_t *) malloc(sizeof(uint8_t) * size);
        glReadPixels(0, 0, width, height * 3 / 8, GL_RGBA, GL_UNSIGNED_BYTE, data);
        return data;
    }
}

void release() {
    glDeleteBuffers(2, mPboIds);

    if (mEGLDisplay != EGL_NO_DISPLAY) {
        eglDestroySurface(mEGLDisplay, mEGLSurface);
        eglDestroyContext(mEGLDisplay, mEGLContext);
        eglReleaseThread();
        eglTerminate(mEGLDisplay);
        glDeleteTextures(mTextureSize, mTextures);
    }
    mEGLDisplay = EGL_NO_DISPLAY;
    mEGLContext = EGL_NO_CONTEXT;
    mEGLSurface = EGL_NO_SURFACE;
}