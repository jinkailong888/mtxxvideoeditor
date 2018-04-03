//
// Created by wyh3 on 2018/3/26.

//

#ifndef MTXXVIDEOEDITOR_FF_FFMUX_SOFT_H
#define MTXXVIDEOEDITOR_FF_FFMUX_SOFT_H

#include <ffmpeg/output/armv7a/include/libavutil/frame.h>
#include <android/log.h>
#include "libavformat/avformat.h"
#include "libavutil/opt.h"
#include "../ijksdl_def.h"

#define TAG "VideoEditor"
#define logd(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__)
#define loge(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__)

void ff_ffmux_soft_release();

void ff_ffmux_soft_init(AVFormatContext *in_fmt_ctx, AVCodecContext *video_dec_ctx,
                        AVCodecContext *audio_enc_ctx,
                        EditorState *es);

void ff_ffmux_soft_onVideoEncode(unsigned char *data, double pts, int size, int width, int height);

void ff_ffmux_soft_onAudioEncode(AVFrame *frame);

int ff_ffmux_soft_onFrameEncode(AVFrame *frame, int *got_frame);

void ff_ffmux_soft_onVideoEncodeDone();

void ff_ffmux_soft_onAudioEncodeDone();



#endif //MTXXVIDEOEDITOR_FF_FFMUX_SOFT_H
