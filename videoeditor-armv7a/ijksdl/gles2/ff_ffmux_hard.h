//
// Created by wyh3 on 2018/3/26.
//

#ifndef MTXXVIDEOEDITOR_FF_FFMUX_HARD_H
#define MTXXVIDEOEDITOR_FF_FFMUX_HARD_H

#include <stdbool.h>
#include <jni.h>
#include <android/log.h>

void ff_ffmux_hard_init(JavaVM *vm);

void ff_ffmux_set_HardMuxJni(JNIEnv *env, jobject instance, jobject filter);

void init_hard();

void release_hard();

void onVideoEncode(uint8_t * data, double pts);


void onVideoEncodeDone();


void onAudioEncode();


void onAudioEncodeDone();


#endif //MTXXVIDEOEDITOR_FF_FFMUX_HARD_H
