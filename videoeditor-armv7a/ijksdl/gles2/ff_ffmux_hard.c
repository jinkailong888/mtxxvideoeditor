//
// Created by wyh3 on 2018/3/26.
//


#include "ff_ffmux_hard.h"
#include "ff_ffmux.h"

static jobject mHardMuxJni;
static jmethodID onVideoEncodeMethod;
static jmethodID onAudioEncodeMethod;
static jmethodID onVideoDoneMethod;
static jmethodID onAudioDoneMethod;
static JavaVM *g_jvm;
static JNIEnv *mEnv;


void ff_ffmux_hard_init(JavaVM *vm) {
    g_jvm = vm;
}

void ff_ffmux_set_HardMuxJni(JNIEnv *env, jobject instance, jobject filter) {

    logd("ff_ffmux_set_HardMuxJni");

    if (mHardMuxJni) {
        (*env)->DeleteGlobalRef(env, mHardMuxJni);
    }
    if (filter != NULL) {
        mHardMuxJni = (*env)->NewGlobalRef(env, filter);
    }
}


void init_hard() {

}

void release_hard() {

}


void onVideoEncode(uint8_t *data, double pts) {

    int status = (*g_jvm)->AttachCurrentThread(g_jvm, &mEnv, NULL);
    logd("onVideoEncode");
    if (status == JNI_OK) {
        logd("onVideoEncode JNI_OK");
        if (onVideoEncodeMethod) {
            (*mEnv)->CallVoidMethod(mEnv, mHardMuxJni, onVideoEncodeMethod, data, pts);
        } else {
            jclass filterClass = (*mEnv)->GetObjectClass(mEnv, mHardMuxJni);
            onVideoEncodeMethod = (*mEnv)->GetMethodID(mEnv, filterClass, "onVideoFrame", "([BD)V");
            (*mEnv)->CallVoidMethod(mEnv, mHardMuxJni, onVideoEncodeMethod, data, pts);
        }
        (*g_jvm)->DetachCurrentThread(g_jvm);
    }
}

void onVideoEncodeDone() {

}


void onAudioEncode() {

}


void onAudioEncodeDone() {

}

