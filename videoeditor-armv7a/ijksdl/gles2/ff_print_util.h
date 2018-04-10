//
// Created by wyh3 on 2018/4/4.
//

#ifndef MTXXVIDEOEDITOR_FF_PRINT_UTIL_H
#define MTXXVIDEOEDITOR_FF_PRINT_UTIL_H


#include <android/log.h>
#include "../../ffmpeg/output/armv7a/include/libavutil/frame.h"
#include "../../ijkplayer/ff_ffaudio_resample.h"

#define TAG "VideoEditor"
#define logd(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)
#define loge(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__)

void print_avframe(AVFrame *frame);

void print_avpacket(AVPacket *avPacket);

void print_avpacket_tag(AVPacket *avPacket, char *tag);

void print_avframe_tag(AVFrame *frame, char *tag);

void print_AVRational(AVRational avRational, char *tag);


void print_audio_codecCtx_tag(AVCodecContext *audio_codec_ctx,char*tag);


#endif //MTXXVIDEOEDITOR_FF_PRINT_UTIL_H
