//
// Created by wyh3 on 2018/3/26.
//

#ifndef MTXXVIDEOEDITOR_FF_FFMUX_HARD_H
#define MTXXVIDEOEDITOR_FF_FFMUX_HARD_H

#include <stdbool.h>
#include <jni.h>
#include <android/log.h>

void ff_ffmux_hard_init(JavaVM *vm,JNIEnv * env);

void ff_ffmux_set_HardMuxJni(JNIEnv *env, jobject instance, jobject filter);

void init_hard();

void release_hard();

void onVideoEncode(unsigned char *data, double pts, int size,int w,int h);


void onVideoEncodeDone();


void onAudioEncode();


void onAudioEncodeDone();


#endif //MTXXVIDEOEDITOR_FF_FFMUX_HARD_H
