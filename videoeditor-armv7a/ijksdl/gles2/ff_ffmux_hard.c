//
// Created by wyh3 on 2018/3/26.
//


#include <string.h>
#include "ff_ffmux_hard.h"
#include "ff_ffmux.h"

static jobject mHardMuxJni;
static jmethodID onVideoEncodeMethod = NULL;
static jmethodID onAudioEncodeMethod;
static jmethodID onVideoDoneMethod;
static jmethodID onAudioDoneMethod;
static JavaVM *g_jvm;
static JNIEnv *mEnv;


void ff_ffmux_hard_init(JavaVM *vm, JNIEnv *env) {
    g_jvm = vm;
    mEnv = env;
}

void ff_ffmux_set_HardMuxJni(JNIEnv *env, jobject instance, jobject hardMuxJni) {

    logd("ff_ffmux_set_HardMuxJni");

    if (mHardMuxJni) {
        (*env)->DeleteGlobalRef(env, mHardMuxJni);
    }
    if (hardMuxJni != NULL) {
        mHardMuxJni = (*env)->NewGlobalRef(env, hardMuxJni);
        logd("ff_ffmux_set_HardMuxJni NewGlobalRef hardMuxJni");
    }


}


void init_hard() {

}

void release_hard() {

}


void onVideoEncode(unsigned char *data, double pts, int size) {
    JNIEnv *env;
    logd("onVideoEncode size=%d",size);

    int status = (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);

    if (status != JNI_OK) {
        loge("onVideoEncode JNI NOT OK!");
        return;
    }

    if (onVideoEncodeMethod == NULL) {
        jclass hardMuxJniClass = (*env)->GetObjectClass(env, mHardMuxJni);
        onVideoEncodeMethod = (*env)->GetMethodID(env, hardMuxJniClass, "onVideoFrame", "([BD)V");
    }

    if (onVideoEncodeMethod != NULL) {
        logd("onVideoEncodeMethod != null");
        jbyteArray array = (*env)->NewByteArray(env, size);
        (*env)->SetByteArrayRegion(env, array, 0, size, (const jbyte *) data);
        if (array == NULL) {
            loge("array = null");
            return;
        }
        (*env)->CallVoidMethod(env, mHardMuxJni, onVideoEncodeMethod, array, pts);
    }

    (*g_jvm)->DetachCurrentThread(g_jvm);
}

void onVideoEncodeDone() {
    JNIEnv *env;
    logd("onVideoEncodeDone ");

    int status = (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);
    if (status != JNI_OK) {
        loge("onVideoEncodeDone JNI NOT OK!");
        return;
    }
    if (onVideoDoneMethod == NULL) {
        jclass hardMuxJniClass = (*env)->GetObjectClass(env, mHardMuxJni);
        onVideoDoneMethod = (*env)->GetMethodID(env, hardMuxJniClass, "onVideoDone", "()V");
    }

    if (onVideoDoneMethod != NULL) {
        logd("CallVoidMethod onVideoDoneMethod");
        (*env)->CallVoidMethod(env, mHardMuxJni, onVideoDoneMethod);
    }

    (*g_jvm)->DetachCurrentThread(g_jvm);
}


void onAudioEncode() {

}


void onAudioEncodeDone() {

}

