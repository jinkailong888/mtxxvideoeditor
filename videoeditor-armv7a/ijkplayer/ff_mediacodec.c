//
//  /**** MeiTu 视频保存 ****/
// Created by wyh3 on 2018/3/13.
//

#include "ff_mediacodec.h"
#include "ffmpeg/ffmpeg-armv7a/libavcodec/mediacodec_wrapper.h"
#include "ffmpeg/ffmpeg-armv7a/libavcodec/mediacodecdec_common.h"
#include "ff_mediacodec_common.h"

#define MY_TAG  "ffeditor"

#define loge(format, ...)  __android_log_print(ANDROID_LOG_ERROR, MY_TAG, format, ##__VA_ARGS__)
#define logd(format, ...)  __android_log_print(ANDROID_LOG_DEBUG,  MY_TAG, format, ##__VA_ARGS__)


#define INPUT_DEQUEUE_TIMEOUT_US 8000
#define OUTPUT_DEQUEUE_TIMEOUT_US 8000
#define OUTPUT_DEQUEUE_BLOCK_TIMEOUT_US 1000000


int mediacodec_encode_init(EditorState *es) {

    int ret;
    const char *codec_mime = "video/avc";

    es->mediaformat = ff_AMediaFormat_new();

    if (!es->mediaformat) {
        loge("Failed to create media format\n");
        ret = AVERROR_EXTERNAL;
        goto done;
    }

    ff_AMediaFormat_setString(es->mediaformat, "mime", codec_mime);
    ff_AMediaFormat_setInt32(es->mediaformat, "width", 1280);
    ff_AMediaFormat_setInt32(es->mediaformat, "height", 720);

    ff_AMediaFormat_setInt32(es->mediaformat, "color-format", 21);
    ff_AMediaFormat_setInt32(es->mediaformat, "bitrate", 2000001);
    ff_AMediaFormat_setInt32(es->mediaformat, "frame-rate", 30);
    ff_AMediaFormat_setInt32(es->mediaformat, "i-frame-interval", 1);

    es->mediaCodec = ff_AMediaCodec_createEncoderByType(codec_mime);
    if (!es->mediaCodec) {
        loge("Failed to create media encoder for type %s \n", codec_mime);
        ret = AVERROR_EXTERNAL;
        goto done;
    } else {
        logd("create media encoder for type %s \n", codec_mime);
    }

    // CONFIGURE_FLAG_ENCODE = 1
    ret = ff_AMediaCodec_configure(es->mediaCodec, es->mediaformat, NULL, NULL, 1);
    if (ret < 0) {
        char *desc = ff_AMediaFormat_toString(es->mediaformat);
        loge("Failed to configure codec (status = %d) with format %s\n", ret, desc);
        av_freep(&desc);
        ret = AVERROR_EXTERNAL;
        goto done;
    }

    ret = ff_AMediaCodec_start(es->mediaCodec);
    if (ret < 0) {
        char *desc = ff_AMediaFormat_toString(es->mediaformat);
        loge("Failed to start codec (status = %d) with format %s\n", ret, desc);
        av_freep(&desc);
        ret = AVERROR_EXTERNAL;
        goto done;
    }

    logd("MediaCodec %p started successfully\n", es->mediaCodec);


    done:
    if (es->mediaformat) {
        ff_AMediaFormat_delete(es->mediaformat);
    }
    if (ret < 0) {
        mediacodec_encode_close();
    }
    return ret;
}


void frame2yuv(EditorState *es, const AVFrame *frame, uint8_t *yuv_data);

void outBuf2avpkt(EditorState *es, uint8_t *yuv_data, AVPacket *avpkt);

int mediacodec_encode_frame(EditorState *es, AVPacket *avpkt, const AVFrame *frame) {


    logd("mediacodec_encode_frame\n");

    int ret;
    uint8_t *data;
    ssize_t in_buf_id;
    ssize_t out_buf_id;
    size_t size;
    FFAMediaCodecBufferInfo info = {0};


    in_buf_id = ff_AMediaCodec_dequeueInputBuffer(es->mediaCodec, INPUT_DEQUEUE_TIMEOUT_US);


    if (in_buf_id < 0) {
        loge("Failed to dequeue input buffer (status=%zd)\n", in_buf_id);
        return AVERROR_EXTERNAL;
    } else {
        logd("成功获取input buffer \n", in_buf_id);
        data = ff_AMediaCodec_getInputBuffer(es->mediaCodec, (size_t) in_buf_id, &size);
        int video_decode_size = avpicture_get_size(es->pix_fmt, es->outputWidth,
                                                   es->outputHeight);
        uint8_t *yuv_data = (uint8_t *) calloc(1, video_decode_size * 3 * sizeof(char));

        //AVFrame中提取出yuv
        logd("AVFrame中提取出yuv \n");
        frame2yuv(es, frame, yuv_data);

        //将yuv数据添加到inputBuffer
        logd("将yuv数据添加到inputBuffer \n");
        memcpy(data, yuv_data, size);

        //将inputBuffer添加到编码队列
        logd("将inputBuffer添加到编码队列 \n");
        ret = ff_AMediaCodec_queueInputBuffer(es->mediaCodec, in_buf_id, 0, size, frame->pts,
                                              0);

        if (ret < 0) {
            loge("Failed to queue input buffer (status = %d)\n", ret);
            return AVERROR_EXTERNAL;
        }
    }


    while (1) {

        out_buf_id = ff_AMediaCodec_dequeueOutputBuffer(es->mediaCodec, &info,
                                                        OUTPUT_DEQUEUE_TIMEOUT_US);

        if (out_buf_id > 0) {

            logd("Got output buffer %zd"
                         " offset=%"
                         PRIi32
                         " size=%"
                         PRIi32
                         " ts=%"
                         PRIi64
                         " flags=%"
                         PRIu32
                         "\n", out_buf_id, info.offset, info.size,
                 info.presentationTimeUs, info.flags);


            if (info.flags & ff_AMediaCodec_getBufferFlagEndOfStream(es->mediaCodec)) {
//                s->eos = 1;
                logd("s->eos = 1");
            }

            if (info.size) {
                data = ff_AMediaCodec_getOutputBuffer(es->mediaCodec, (size_t) out_buf_id,
                                                      &size);
                if (!data) {
                    loge("Failed to get output buffer\n");
                } else {
                    logd("成功获取编码后的数据");
                    outBuf2avpkt(es, data, avpkt);
                }
                ret = ff_AMediaCodec_releaseOutputBuffer(es->mediaCodec, (size_t) out_buf_id, 0);
                if (ret < 0) {
                    loge("Failed to release output buffer\n");
                }
            } else {
                ret = ff_AMediaCodec_releaseOutputBuffer(es->mediaCodec, out_buf_id, 0);
                if (ret < 0) {
                    logd("Failed to release output buffer\n");
                }
            }
            break;
        } else if (ff_AMediaCodec_infoOutputFormatChanged(es->mediaCodec, out_buf_id)) {
            logd("ff_AMediaCodec_infoOutputFormatChanged");

        } else if (ff_AMediaCodec_infoOutputBuffersChanged(es->mediaCodec, out_buf_id)) {
            logd("ff_AMediaCodec_infoOutputBuffersChanged");

        } else if (ff_AMediaCodec_infoTryAgainLater(es->mediaCodec, out_buf_id)) {
            logd("ff_AMediaCodec_infoTryAgainLater");
        } else {
            logd("Failed to dequeue output buffer (status=%zd)\n", out_buf_id);
        }
    }
    return 0;
}


void frame2yuv(EditorState *es, const AVFrame *frame, uint8_t *yuv_data) {
    int i, j, k;
    if (es->pix_fmt == AV_PIX_FMT_YUV420P) {
        logd("frame2yuv AV_PIX_FMT_YUV420P");
        for (i = 0; i < es->outputHeight; i++) {
            memcpy(yuv_data + es->outputWidth * i,
                   frame->data[0] + frame->linesize[0] * i,
                   (size_t) es->outputWidth);
        }
        for (j = 0; j < es->outputHeight / 2; j++) {
            memcpy(yuv_data + es->outputWidth * i +
                   es->outputWidth / 2 * j,
                   frame->data[1] + frame->linesize[1] * j,
                   (size_t) (es->outputWidth / 2));
        }
        for (k = 0; k < es->outputHeight / 2; k++) {
            memcpy(yuv_data + es->outputWidth * i +
                   es->outputWidth / 2 * j + es->outputWidth / 2 * k,
                   frame->data[2] + frame->linesize[2] * k,
                   (size_t) (es->outputWidth / 2));
        }
    }
}


void outBuf2avpkt(EditorState *es, uint8_t *yuv_data, AVPacket *avpkt) {

    avpkt->data = yuv_data;


}


int mediacodec_encode_close() {


}

