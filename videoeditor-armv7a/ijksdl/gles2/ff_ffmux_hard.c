//
// Created by wyh3 on 2018/3/26.
//


#include <string.h>
#include "ff_ffmux_hard.h"
#include "ff_ffmux.h"

static jobject mHardMuxJni;
static jmethodID onVideoEncodeMethod;
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

    jclass hardMuxJniClass = (*env)->GetObjectClass(env, mHardMuxJni);
    jmethodID m = (*env)->GetMethodID(env, hardMuxJniClass, "onVideoFrame", "([BD)V");
    onVideoEncodeMethod =(*env)->NewGlobalRef(env, m);

}


void init_hard() {

}

void release_hard() {

}


void onVideoEncode(uint8_t *data, double pts) {
    JNIEnv *env;
    logd("onVideoEncode");
    if (onVideoEncodeMethod) {
        logd("onVideoEncodeMethod");
        //获得当前线程可以使用的 JNIEnv *指针
        int status= (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);
        if (status == JNI_OK) {
            logd("JNI_OK");
            (*env)->CallVoidMethod(env, mHardMuxJni, onVideoEncodeMethod, data, pts);
            (*g_jvm)->DetachCurrentThread(g_jvm);
        }
    }
}

void onVideoEncodeDone() {

}


void onAudioEncode() {

}


void onAudioEncodeDone() {

}

