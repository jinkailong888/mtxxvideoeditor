//
// Created by wyh3 on 2018/3/26.
//

#include <stdbool.h>
#include "ff_ffmux.h"
#include "ff_ffmux_hard.h"
#include "ff_ffmux_soft.h"


static bool hard_mux;


void ffmux_init(FFPlayer *ffp) {
    if (ffp->hard_mux) {
        init_hard(ffp);
    } else {
        init_soft(ffp);
    }
    hard_mux = ffp->hard_mux;
}

void ffmux_release() {
    if (hard_mux) {
        release_hard();
    } else {
        release_soft();
    }
}


void video_encode(AVFrame *frame) {
    if (hard_mux) {
        video_encode_hard(frame);
    } else {
        video_encode_soft(frame);
    }
}

void audio_encode(AVFrame *frame) {
    if (hard_mux) {
        audio_encode_hard(frame);
    } else {
        audio_encode_soft(frame);
    }
}
