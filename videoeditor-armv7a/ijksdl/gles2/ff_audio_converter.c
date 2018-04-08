//
// Created by wyh3 on 2018/4/8.
//

#include <stdbool.h>
#include "ff_audio_converter.h"
#include "ff_print_util.h"


static SwrContext *audio_convert_ctx;
static int out_buffer_size;
static int out_channels;
static enum AVSampleFormat out_sample_fmt;
static bool open;


bool ff_audio_converter_isOpen() {
    return open;
}

int ff_audio_converter_open(
        uint64_t src_channel_layout,
        enum AVSampleFormat src_sample_fmt,
        int src_sample_rate,
        uint64_t dst_channel_layout,
        enum AVSampleFormat dst_sample_fmt,
        int dst_sample_rate,
        int dst_nb_samples) {  //nb_samples: AAC-1024 MP3-1152

    if (open) {
        loge("ff_audio_converter_open is open");
        return -1;
    }

//    audio_convert_ctx = swr_alloc();
//
//    if (!audio_convert_ctx) {
//        loge("Failed to swr_alloc");
//        return -1;
//    }

    audio_convert_ctx = swr_alloc_set_opts(
            NULL,
            dst_channel_layout,
            dst_sample_fmt,
            dst_sample_rate,
            src_channel_layout,
            src_sample_fmt,
            src_sample_rate,
            0,
            NULL);

    if (!audio_convert_ctx) {
        loge("Failed to swr_alloc_set_opts");
        return -1;
    }

    if (swr_init(audio_convert_ctx) < 0) {
        loge("Failed to swr_init");
        return -1;
    }

    logd("Success to ff_audio_converter_open");


    out_channels = av_get_channel_layout_nb_channels(dst_channel_layout);

    out_buffer_size = av_samples_get_buffer_size(NULL,
                                                 out_channels,
                                                 dst_nb_samples,
                                                 dst_sample_fmt, 1);

    out_sample_fmt = dst_sample_fmt;

    open = true;
    return 0;
}

uint8_t *ff_audio_converter_convert(const uint8_t **srcData,
                                    int src_nb_samples,
                                    int *resampled_size) {

    uint8_t *audio_out_buffer = (uint8_t *) av_malloc((size_t) out_buffer_size);

    int len = swr_convert(
            audio_convert_ctx,
            &audio_out_buffer,
            out_buffer_size,
            srcData,
            src_nb_samples);

    //每声道采样数 x 声道数 x 每个采样字节数
    int resampled_data_size = len * out_channels * av_get_bytes_per_sample(out_sample_fmt);

    resampled_size = &resampled_data_size;

    return audio_out_buffer;
}


void ff_audio_converter_release() {
    out_buffer_size = 0;
    swr_free(&audio_convert_ctx);
    open = false;
}