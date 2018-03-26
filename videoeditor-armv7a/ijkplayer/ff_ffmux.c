//
// Created by wyh3 on 2018/3/26.
//

#include <stdbool.h>
#include "ff_ffmux.h"
#include "ff_ffmux_hard.h"
#include "ff_ffmux_soft.h"


const bool HARD = false;


void ffmux_init(EditorState *es) {
    if (HARD) {
        init_hard(es);
    } else {
        init_soft(es);
    }
}

void ffmux_release() {
    if (HARD) {
        release_hard();
    } else {
        release_soft();
    }
}


void video_encode(AVFrame *frame) {
    if (HARD) {
        video_encode_hard(frame);
    } else {
        video_encode_soft(frame);
    }
}

void audio_encode(AVFrame *frame) {
    if (HARD) {
        audio_encode_hard(frame);
    } else {
        audio_encode_soft(frame);
    }
}
