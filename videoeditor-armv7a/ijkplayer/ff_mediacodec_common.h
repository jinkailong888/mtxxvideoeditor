//
//  /**** MeiTu 视频保存 ****/
// Created by wyh3 on 2018/3/13.
//

#ifndef MTXXVIDEOEDITOR_FF_MEDIACODEC_COMMON_H
#define MTXXVIDEOEDITOR_FF_MEDIACODEC_COMMON_H


#include <ffmpeg/ffmpeg-armv7a/libavcodec/avcodec.h>
#include <ijksdl/ffmpeg/ijksdl_inc_ffmpeg.h>
#include <ffmpeg/ffmpeg-armv7a/libavcodec/mediacodec_wrapper.h>


typedef struct MediaCodecEncContext {
    FFAMediaCodec *codec;
    FFAMediaFormat *format;
};



int ff_mediacodec_encode_init(const char *mime, FFAMediaFormat *format);

int ff_mediacodec_encode_frame(AVPacket *avpkt, const AVFrame *frame);

void ff_mediacodec_encode_flush();

int ff_mediacodec_encode_close();














#endif //MTXXVIDEOEDITOR_FF_MEDIACODEC_COMMON_H
