//
// Created by wyh3 on 2018/3/26.
//

#ifndef MTXXVIDEOEDITOR_FF_FFMUX_HARD_H
#define MTXXVIDEOEDITOR_FF_FFMUX_HARD_H

#include <ffmpeg/output/armv7a/include/libavutil/frame.h>
#include "ff_ffplay_def.h"

void init_hard(FFPlayer *ffp);

void release_hard();


void video_encode_hard(AVFrame *frame);

void audio_encode_hard(AVFrame *frame);

#endif //MTXXVIDEOEDITOR_FF_FFMUX_HARD_H
