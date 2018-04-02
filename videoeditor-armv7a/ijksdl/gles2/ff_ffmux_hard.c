//
// Created by wyh3 on 2018/3/26.
//


#include <string.h>
#include <malloc.h>
#include <ijkyuv/include/libyuv.h>
#include "ff_ffmux_hard.h"


//void rgb565ToYuv(int width,int height,int size,unsigned char * rgb,unsigned char * yuv){
//    int rgba_stride= ((type & 0xF0) >> 4)*width;
//
//
//
//
//
//    RGBAToI420(rgb,);
//}






void rgbaToYuv(int width, int height, int size, unsigned char *rgb, unsigned char *yuv) {
    int src_stride_rgba = width * 2;
    int y_stride = width;
    int u_stride = (width +1)/2;
    int v_stride = u_stride;
    size_t ySize = (size_t) (y_stride * height);
    size_t uSize = (size_t) (u_stride * height >> 1);

    BGRAToI420(rgb,
               src_stride_rgba, //数据每一行的大小，如果是argb_8888格式的话这个值为wX4，argb4444的话值为wX2
               yuv,//用于保存y分量数据
               y_stride,
               yuv + size,//用于保存u分量数据
               u_stride,
               yuv + size + u_stride * (height + 1) / 2,//用于保存分量数据
               v_stride,
               width,
               height
    );
}


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



void ff_ffmux_hard_release() {

}


void ff_ffmux_hard_onVideoEncode(unsigned char *data, double pts, int size, int width, int height) {
    JNIEnv *env;
    logd("onVideoEncode size=%d", size);

    int status = (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);

    if (status != JNI_OK) {
        loge("onVideoEncode JNI NOT OK!");
        return;
    }
    unsigned char *yuv = (unsigned char *) malloc(sizeof(uint8_t) * size);
    rgbaToYuv(width, height, size, data, yuv);

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

void ff_ffmux_hard_onVideoEncodeDone() {
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


void ff_ffmux_hard_onAudioEncode() {

}


void ff_ffmux_hard_onAudioEncodeDone() {

}







