

#include <jni.h>
#include <android/log.h>
#include "gl_jni.h"
#include "gl_util.h"

#define MY_TAG  "opengl"
#define loge(format, ...)  __android_log_print(ANDROID_LOG_ERROR, MY_TAG, format, ##__VA_ARGS__)
#define logd(format, ...)  __android_log_print(ANDROID_LOG_DEBUG,  MY_TAG, format, ##__VA_ARGS__)


static JNIEnv *mEnv;
static jobject mFilter;
static jmethodID onCreatedMethod;
static jmethodID onSizeChangedMethod;
static jmethodID onDrawFrameMethod;
static JavaVM *g_jvm;


void gl_jni_init(JNIEnv *env,JavaVM *jvm) {
    mEnv = env;
    g_jvm = jvm;
}


void onCreated() {
    logd(" onCreated");
    if (!mEnv) {
        logd("onCreated !mEnv");
        int status = (*g_jvm)->AttachCurrentThread(g_jvm, &mEnv, NULL);
        logd("onCreated AttachCurrentThread");
        if (status == JNI_OK) {
            logd("onCreated JNI_OK");
            jclass filterClass = (*mEnv)->GetObjectClass(mEnv, mFilter);
            onCreatedMethod = (*mEnv)->GetMethodID(mEnv, filterClass, "surfaceCreated", "()V");
            onSizeChangedMethod = (*mEnv)->GetMethodID(mEnv, filterClass, "onSizeChanged", "(II)V");
            onDrawFrameMethod = (*mEnv)->GetMethodID(mEnv, filterClass, "onDrawFrame", "([I)V");
            logd("onCreated GetMethodID");
            (*g_jvm)->DetachCurrentThread(g_jvm);
            logd("onCreated DetachCurrentThread");
        }
    }
    if (onCreatedMethod) {
        logd("onCreated onCreatedMethod");
        int status = (*g_jvm)->AttachCurrentThread(g_jvm, &mEnv, NULL);
        if (status == JNI_OK) {
            (*mEnv)->CallVoidMethod(mEnv, mFilter, onCreatedMethod);
            (*g_jvm)->DetachCurrentThread(g_jvm);
        }
    }
}

void onSizeChanged(int width, int height) {
    if (onSizeChangedMethod) {
        (*g_jvm)->AttachCurrentThread(g_jvm, &mEnv, NULL);
        (*mEnv)->CallVoidMethod(mEnv, mFilter, onSizeChangedMethod, width, height);
        (*g_jvm)->DetachCurrentThread(g_jvm);
    }
}

void onDrawFrame(int *textureId) {
    if (onDrawFrameMethod) {
        (*g_jvm)->AttachCurrentThread(g_jvm, &mEnv, NULL);
        (*mEnv)->CallIntMethod(mEnv, mFilter, onDrawFrameMethod, textureId);
        (*g_jvm)->DetachCurrentThread(g_jvm);
    }
}

/*注意不能直接保存env，filter然后在onDrawFrame等方法中使用，因为这三个方法的调用与setGLFilter不是在同一个线程中*/
void gl_jni_setGLFilter(JNIEnv *env, jobject instance, jobject filter) {
    if (mFilter) {
        (*env)->DeleteGlobalRef(env, mFilter);
    }
    if (filter != NULL) {
        mFilter = (*env)->NewGlobalRef(env, filter);
    }

    logd("gl_jni_setGLFilter setupEGL");
    setupEGL(720, 1280, 3);
    onCreated();
    logd("gl_jni_setGLFilter onSizeChanged");
    onSizeChanged(720, 1280);


}
