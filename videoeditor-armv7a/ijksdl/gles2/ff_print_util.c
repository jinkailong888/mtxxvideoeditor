//
// Created by wyh3 on 2018/4/4.
//

#include "ff_print_util.h"


void print_avframe(AVFrame *frame) {
    if (frame) {
        logd("frame->width=%d", frame->width);
        logd("frame->height=%d", frame->height);
        logd("frame->format=%d", frame->format);
        logd("frame->pts=%lld", frame->pts);
        logd("frame->pkt_dts=%lld", frame->pkt_dts);
        logd("frame->pkt_size=%d", frame->pkt_size);
        logd("frame->linesize[0]=%d", frame->linesize[0]);
        logd("frame->linesize[1]=%d", frame->linesize[1]);
        logd("frame->linesize[2]=%d", frame->linesize[2]);
    } else {
        logd("print_avframe frame is null");
    }
}

void print_avframe_tag(AVFrame *frame, char *tag) {
    if (frame) {
        logd("%s frame->width=%d", tag, frame->width);
        logd("%s frame->height=%d", tag, frame->height);
        logd("%s frame->format=%d", tag, frame->format);
        logd("%s frame->pts=%lld", tag, frame->pts);
        logd("%s frame->pkt_dts=%lld", tag, frame->pkt_dts);
        logd("%s frame->pkt_size=%d", tag, frame->pkt_size);
        logd("%s frame->linesize[0]=%d", tag, frame->linesize[0]);
        logd("%s frame->linesize[1]=%d", tag, frame->linesize[1]);
        logd("%s frame->linesize[2]=%d", tag, frame->linesize[2]);
    } else {
        logd("%s  print_avframe frame is null", tag);
    }
}


void print_avpacket(AVPacket *avPacket) {
    if (avPacket) {
        logd("enc_pkt->size=%d", avPacket->size);
        logd("enc_pkt->pts=%lld", avPacket->pts);
        logd("enc_pkt->dts=%lld", avPacket->dts);
        logd("enc_pkt->duration=%lld", avPacket->duration);
    } else {
        logd("print_avpacket avPacket is null");
    }
}

void print_avpacket_tag(AVPacket *avPacket, char *tag) {
    if (avPacket) {
        logd("%s enc_pkt->size=%d", tag, avPacket->size);
        logd("%s enc_pkt->pts=%lld", tag, avPacket->pts);
        logd("%s enc_pkt->dts=%lld", tag, avPacket->dts);
        logd("%s enc_pkt->duration=%lld", tag, avPacket->duration);
    } else {
        logd("%s print_avpacket avPacket is null", tag);
    }
}
