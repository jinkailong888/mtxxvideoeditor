//
//  /**** MeiTu 视频保存 ****/
// Created by wyh3 on 2018/3/13.
//

#include "ff_mediacodec_common.h"
#define MY_TAG  "ffeditor"

#define loge(format, ...)  __android_log_print(ANDROID_LOG_ERROR, MY_TAG, format, ##__VA_ARGS__)
#define logd(format, ...)  __android_log_print(ANDROID_LOG_DEBUG,  MY_TAG, format, ##__VA_ARGS__)


int ff_mediacodec_encode_init(const char *mime, FFAMediaFormat *format){

    ff_AMediaCodec_createEncoderByType(mime);


}
