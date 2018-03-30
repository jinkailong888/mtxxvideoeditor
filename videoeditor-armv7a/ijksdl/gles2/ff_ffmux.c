//
// Created by wyh3 on 2018/3/26.
//

#include <stdbool.h>
#include "ff_ffmux.h"
#include "ff_ffmux_hard.h"


static bool ffmux_hard_mux;


void ffmux_init(bool hard_mux) {
    if (hard_mux) {
        init_hard();
    } else {
//        init_soft();
    }
    ffmux_hard_mux = hard_mux;
}

void ffmux_release() {
    if (ffmux_hard_mux) {
        release_hard();
    } else {
//        release_soft();
    }
}


void ffmux_video_encode(unsigned char *data, double pts, int size,int width,int height) {
    if (ffmux_hard_mux) {
        onVideoEncode(data, pts, size,width,height);
    } else {

    }

}


void ffmux_video_encode_done() {
    if (ffmux_hard_mux) {
        onVideoEncodeDone();
    } else {

    }

}
