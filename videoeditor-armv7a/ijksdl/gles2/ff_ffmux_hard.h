//
// Created by wyh3 on 2018/3/26.
//

#ifndef MTXXVIDEOEDITOR_FF_FFMUX_HARD_H
#define MTXXVIDEOEDITOR_FF_FFMUX_HARD_H

#include <ffmpeg/output/armv7a/include/libavutil/frame.h>
#include "libavformat/avformat.h"
#include "libavutil/opt.h"
#include <stdbool.h>
#include <jni.h>
#include <string.h>
#include <malloc.h>

#include "ff_print_util.h"
#include "ff_converter.h"
#include "../ijksdl_def.h"

void ff_ffmux_hard_init(JavaVM *vm,JNIEnv * env);

void ff_ffmux_set_HardMuxJni(JNIEnv *env, jobject instance, jobject filter);


void ff_ffmux_hard_release();

void ff_ffmux_hard_onVideoEncode(unsigned char *data, double pts, int size,int w,int h);


void ff_ffmux_hard_onVideoEncodeDone();


void ff_ffmux_hard_onAudioEncode(AVFrame *pFrame);


void ff_ffmux_hard_onAudioEncodeDone();


#endif //MTXXVIDEOEDITOR_FF_FFMUX_HARD_H
