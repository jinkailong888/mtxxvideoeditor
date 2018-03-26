//
// Created by wyh3 on 2018/3/26.
//

#ifndef MTXXVIDEOEDITOR_FF_FFMUX_SOFT_H
#define MTXXVIDEOEDITOR_FF_FFMUX_SOFT_H
#include <ffmpeg/output/armv7a/include/libavutil/frame.h>
#include "ff_ffplay_def.h"

void init_soft(EditorState *es);

void release_soft();

void video_encode_soft(AVFrame *frame);

void audio_encode_soft(AVFrame *frame);

#endif //MTXXVIDEOEDITOR_FF_FFMUX_SOFT_H
