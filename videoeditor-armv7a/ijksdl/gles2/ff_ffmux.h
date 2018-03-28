//
// Created by wyh3 on 2018/3/26.
//

#ifndef MTXXVIDEOEDITOR_FF_FFMUX_H
#define MTXXVIDEOEDITOR_FF_FFMUX_H


#include <stdint.h>

#define MY_TAG  "VideoEditor"

#define loge(format, ...)  __android_log_print(ANDROID_LOG_ERROR, MY_TAG, format, ##__VA_ARGS__)
#define logd(format, ...)  __android_log_print(ANDROID_LOG_DEBUG,  MY_TAG, format, ##__VA_ARGS__)


void ffmux_init(bool b);

void ffmux_release();

void ffmux_video_encode(uint8_t * data, double pts);




#endif //MTXXVIDEOEDITOR_FF_FFMUX_H
