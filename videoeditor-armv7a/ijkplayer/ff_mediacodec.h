//
//  /**** MeiTu 视频保存 ****/
// Created by wyh3 on 2018/3/13.
//

#include <ijksdl/ffmpeg/ijksdl_inc_ffmpeg.h>
#include "ff_ffplay_def.h"

#ifndef MTXXVIDEOEDITOR_FF_MEDIACODEC_H
#define MTXXVIDEOEDITOR_FF_MEDIACODEC_H


int mediacodec_encode_init(EditorState *es);

int mediacodec_encode_frame(EditorState *es, AVPacket *avpkt, const AVFrame *frame);

void mediacodec_encode_flush();

int mediacodec_encode_close();


#endif; //MTXXVIDEOEDITOR_FF_MEDIACODEC_H
