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


void ffmux_video_encode(uint8_t * data, double pts){
    if (ffmux_hard_mux) {
        onVideoEncode(data, pts);
    } else {

    }

}
