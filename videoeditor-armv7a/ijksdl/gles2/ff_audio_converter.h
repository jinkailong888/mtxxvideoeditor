//
// Created by wyh3 on 2018/4/8.
//

#ifndef MTXXVIDEOEDITOR_FF_AUDIO_CONVERTER_H
#define MTXXVIDEOEDITOR_FF_AUDIO_CONVERTER_H

#include <ffmpeg/output/armv7a/include/libavutil/frame.h>
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"
#include "libavcodec/avcodec.h"
#include "libswresample/swresample.h"

bool ff_audio_converter_isOpen();
int ff_audio_converter_open(uint64_t src_channel_layout,
                            enum AVSampleFormat src_sample_fmt,
                            int src_sample_rate,
                            uint64_t dst_channel_layout,
                            enum AVSampleFormat dst_sample_fmt,
                            int dst_sample_rate,
                            int dst_nb_samples);


uint8_t *ff_audio_converter_convert(const uint8_t **srcData,
                                    int src_nb_samples,
                                    int *resampled_size);


void ff_audio_converter_release();


#endif //MTXXVIDEOEDITOR_FF_AUDIO_CONVERTER_H
