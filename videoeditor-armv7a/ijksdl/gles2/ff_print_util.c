//
// Created by wyh3 on 2018/4/4.
//

#include <stdbool.h>
#include "ff_print_util.h"

const bool print = true;

void print_avframe(AVFrame *frame) {
    if (!print) {
        return;
    }
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
        loge("print_avframe frame is null");
    }
}

void print_avframe_tag(AVFrame *frame, char *tag) {
    if (!print) {
        return;
    }
    if (frame) {
//        logd("%s frame->width=%d", tag, frame->width);
//        logd("%s frame->height=%d", tag, frame->height);
//        logd("%s frame->format=%d", tag, frame->format);
        logd("%s frame->pts=%lld", tag, frame->pts);
        logd("%s frame->nb_samples=%d", tag, frame->nb_samples);
//        logd("%s frame->pkt_dts=%lld", tag, frame->pkt_dts);
        logd("%s frame->pkt_size=%d", tag, frame->pkt_size);
        logd("%s frame->linesize[0]=%d", tag, frame->linesize[0]);
//        logd("%s frame->linesize[1]=%d", tag, frame->linesize[1]);
//        logd("%s frame->linesize[2]=%d", tag, frame->linesize[2]);
    } else {
        loge("%s  print_avframe frame is null", tag);
    }
}


void print_avpacket(AVPacket *avPacket) {
    if (!print) {
        return;
    }
    if (avPacket) {
        logd("avPacket->size=%d", avPacket->size);
        logd("avPacket->pts=%lld", avPacket->pts);
        logd("avPacket->dts=%lld", avPacket->dts);
        logd("avPacket->duration=%lld", avPacket->duration);
    } else {
        loge("print_avpacket avPacket is null");
    }
}

void print_avpacket_tag(AVPacket *avPacket, char *tag) {
    if (!print) {
        return;
    }
    if (avPacket) {
        logd("%s avPacket->size=%d", tag, avPacket->size);
        logd("%s avPacket->pts=%lld", tag, avPacket->pts);
        logd("%s avPacket->dts=%lld", tag, avPacket->dts);
        logd("%s avPacket->duration=%lld", tag, avPacket->duration);
    } else {
        loge("%s print_avpacket avPacket is null", tag);
    }
}


void print_AVRational(AVRational avRational, char *tag) {
    if (!print) {
        return;
    }
    if (&avRational) {
        logd("%s num/den ：%d/%d", tag, avRational.num, avRational.den);
    }else{
        loge("%s print_AVRational avRational is null", tag);
    }
}


void print_audio_codecCtx_tag(AVCodecContext *audio_codec_ctx,char*tag){
    if (!print) {
        return;
    }
    if (audio_codec_ctx) {
        logd("%s audio_codec_ctx->sample_rate=%d", tag, audio_codec_ctx->sample_rate);
        logd("%s audio_codec_ctx->channel_layout=%lld", tag, audio_codec_ctx->channel_layout);
        logd("%s audio_codec_ctx->sample_fmt=%d", tag, audio_codec_ctx->sample_fmt);
        print_AVRational(audio_codec_ctx->time_base, tag);
    }else{
        loge("%s print_audio_codecCtx_tag , audio_codec_ctx is null", tag);
    }



}
