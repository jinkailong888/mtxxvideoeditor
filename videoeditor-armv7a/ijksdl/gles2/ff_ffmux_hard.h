//
// Created by wyh3 on 2018/3/26.
//

#ifndef MTXXVIDEOEDITOR_FF_FFMUX_HARD_H
#define MTXXVIDEOEDITOR_FF_FFMUX_HARD_H

#include <stdbool.h>
#include <jni.h>
#include <android/log.h>

#define TAG "VideoEditor"
#define logd(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)
#define loge(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__)

void ff_ffmux_hard_init(JavaVM *vm,JNIEnv * env);

void ff_ffmux_set_HardMuxJni(JNIEnv *env, jobject instance, jobject filter);


void ff_ffmux_hard_release();

void ff_ffmux_hard_onVideoEncode(unsigned char *data, double pts, int size,int w,int h);


void ff_ffmux_hard_onVideoEncodeDone();


void ff_ffmux_hard_onAudioEncode();


void ff_ffmux_hard_onAudioEncodeDone();


#endif //MTXXVIDEOEDITOR_FF_FFMUX_HARD_H
