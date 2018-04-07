//
// Created by wyh3 on 2018/3/26.

//

#ifndef MTXXVIDEOEDITOR_FF_FFMUX_SOFT_H
#define MTXXVIDEOEDITOR_FF_FFMUX_SOFT_H

#include <ffmpeg/output/armv7a/include/libavutil/frame.h>
#include "libavformat/avformat.h"
#include "libavutil/opt.h"
#include "../ijksdl_def.h"
#include "ff_print_util.h"
#include "ff_converter.h"
#include <libavfilter/avfiltergraph.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>

void ff_ffmux_soft_release();

void ff_ffmux_soft_init(AVFormatContext *in_fmt_ctx, AVCodecContext *video_dec_ctx,
                        AVCodecContext *audio_enc_ctx,
                        EditorState *es);

void ff_ffmux_soft_onVideoEncode(unsigned char *rgbaData, int64_t pts, int64_t size, int format,
                                 int width, int height, int i, AVDictionary *pDictionary,
                                 enum AVColorRange range, enum AVColorPrimaries primaries,
                                 enum AVColorTransferCharacteristic characteristic,
                                 enum AVColorSpace space, enum AVChromaLocation location, int i1,
                                 int64_t i2);

int ff_ffmux_soft_onAudioEncode(AVFrame *frame,int *got_frame);

int ff_ffmux_soft_onVideoFrameEncode(AVFrame *frame);

void ff_ffmux_soft_onVideoEncodeDone();

void ff_ffmux_soft_onAudioEncodeDone();



#endif //MTXXVIDEOEDITOR_FF_FFMUX_SOFT_H
