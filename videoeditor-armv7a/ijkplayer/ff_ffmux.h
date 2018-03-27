//
// Created by wyh3 on 2018/3/26.
//

#ifndef MTXXVIDEOEDITOR_FF_FFMUX_H
#define MTXXVIDEOEDITOR_FF_FFMUX_H


#include <ffmpeg/output/armv7a/include/libavutil/frame.h>
#include "ff_ffplay_def.h"

#define MY_TAG  "VideoEditor"

#define loge(format, ...)  __android_log_print(ANDROID_LOG_ERROR, MY_TAG, format, ##__VA_ARGS__)
#define logd(format, ...)  __android_log_print(ANDROID_LOG_DEBUG,  MY_TAG, format, ##__VA_ARGS__)

struct Mux_Encoder {
    int (*video_encode)(AVFrame *frame);

    int (*audio_encode)(AVFrame *frame);
};

void video_encode(AVFrame *frame);

void audio_encode(AVFrame *frame);

void ffmux_init(FFPlayer *ffp);

void ffmux_release();


#endif //MTXXVIDEOEDITOR_FF_FFMUX_H
