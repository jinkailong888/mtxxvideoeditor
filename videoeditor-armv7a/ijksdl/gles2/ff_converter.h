//
// Created by yhao on 2018/4/7.
//

#ifndef MTXXVIDEOEDITOR_FF_CONVERTER_H
#define MTXXVIDEOEDITOR_FF_CONVERTER_H


#include <ffmpeg/output/armv7a/include/libavutil/frame.h>
#include "libavformat/avformat.h"
#include "libswscale/swscale.h"
#include "libavcodec/avcodec.h"



//Image Format
#define CV_FMT_YU12     0  //Not supported
#define CV_FMT_YV12     1
#define CV_FMT_NV21     2
#define CV_FMT_NV12     3
#define CV_FMT_YUYV422  4
#define CV_FMT_YVYU422  5
#define CV_FMT_YUV422P  6
#define CV_FMT_UYVY422  7
#define CV_FMT_GRAY8    8
#define CV_FMT_RGB565   9
#define CV_FMT_RGB888   10
#define CV_FMT_ARGB8888 11
#define CV_FMT_ABGR8888 12
#define CV_FMT_BGRA8888 13

//SWS Algorithm
#define CV_ALG_FAST_BILINEAR 0
#define CV_ALG_BILINEAR      1
#define CV_ALG_BICUBIC       2
#define CV_ALG_X             3
#define CV_ALG_POINT         4
#define CV_ALG_AREA          5
#define CV_ALG_BICUBLIN      6
#define CV_ALG_GAUSS         7
#define CV_ALG_SINC          8
#define CV_ALG_LANCZOS       9
#define CV_ALG_SPLINE        10

typedef long FF_CONVERTER;

FF_CONVERTER ff_converter_open(int src_width, int src_height, int src_fmt, int dst_width, int dst_height, int dst_fmt, int alg);
void ff_converter_close(FF_CONVERTER fd);
int ff_converter_process(FF_CONVERTER fd, unsigned char *src, int src_size, unsigned char *dst, int dst_size);
int ff_converter_calc_src_size(FF_CONVERTER fd);
int ff_converter_calc_dst_size(FF_CONVERTER fd);
int ff_converter_calc_size(int width, int height, int fmt);

int ff_converter_process_once(int src_width, int src_height, int src_fmt, int dst_width, int dst_height, int dst_fmt, int alg,
                              unsigned char *src, int src_size, unsigned char *dst, int dst_size);
#endif //MTXXVIDEOEDITOR_FF_CONVERTER_H
