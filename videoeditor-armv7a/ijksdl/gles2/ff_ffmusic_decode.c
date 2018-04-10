//
// Created by wyh3 on 2018/4/8.
//

#include "ff_ffmusic_decode.h"
#include "ff_print_util.h"
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>

int (*mSendFrameFun)(AVFrame *, enum AVSampleFormat format);

static int open_input_file(const char *filename);

int ffmusic_decode_thread(void *arg);

static AVFormatContext *ifmt_ctx;
static AVCodecContext *audio_dec_ctx;
static int audio_stream_index = -1;
static enum AVSampleFormat sample_fmt;

void ffmusic_decode_start(EditorState *es) {

    logd("ffmusic_decode_start");

    es->music_decode_tid = SDL_CreateThreadEx(&es->_music_decode_tid, ffmusic_decode_thread, es,
                                              "ffmusic_decode_thread");
    if (!es->music_decode_tid) {
        loge("SDL_CreateThread(): %s\n", SDL_GetError());
    }

}


int ffmusic_decode_thread(void *arg) {
    int ret;
    EditorState *es = arg;
    mSendFrameFun = es->sendMusicFramefun;
    ret = open_input_file(es->musicPath);
    if (ret < 0) {
        loge("Failed to open_input_file");
        return 0;
    }
    AVPacket packet = {.data = NULL, .size = 0};
    AVFrame *frame = NULL;
    int got_frame;
    while (1) {
        if ((ret = av_read_frame(ifmt_ctx, &packet)) < 0) {
            loge("Failed to av_read_frame,on error or end of file");
            break;
        }
        frame = av_frame_alloc();
        if (!frame) {
            loge("Failed to av_frame_alloc");
            ret = AVERROR(ENOMEM);
            break;
        }
        av_packet_rescale_ts(&packet,
                             ifmt_ctx->streams[audio_stream_index]->time_base,
                             audio_dec_ctx->time_base);

        avcodec_decode_audio4(audio_dec_ctx, frame, &got_frame, &packet);
        if (ret < 0) {
            av_frame_free(&frame);
            loge("Decoding failed\n");
            break;
        }
        if (got_frame) {
            frame->pts = av_frame_get_best_effort_timestamp(frame);
            mSendFrameFun(frame, sample_fmt);
        } else {
            av_frame_free(&frame);
        }
        av_packet_unref(&packet);
    }


    avcodec_free_context(&audio_dec_ctx);

    audio_stream_index = -1;

    avformat_close_input(&ifmt_ctx);

    return ret;
}


static int open_input_file(const char *filename) {
    int ret;
    unsigned int i;

    AVDictionary *format_opts = NULL;
    av_dict_set(&format_opts, "safe", "0", 0);
    av_dict_set(&format_opts, "protocol_whitelist", "concat,tcp,http,https,tls,file", 0);

    ifmt_ctx = NULL;
    if ((ret = avformat_open_input(&ifmt_ctx, filename, NULL, &format_opts)) < 0) {
        loge("Cannot open input file\n");
        return ret;
    }
    if ((ret = avformat_find_stream_info(ifmt_ctx, NULL)) < 0) {
        loge("Cannot find stream information\n");
        return ret;
    }

    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        AVStream *stream = ifmt_ctx->streams[i];
        AVCodecContext *dec_ctx;
        AVCodec *codec = NULL;

        dec_ctx = avcodec_alloc_context3(NULL);
        if (!dec_ctx) {
            loge("Failed to avcodec_alloc_context3 "
                         "for stream #%u\n", i);
            return AVERROR(ENOMEM);
        }

        ret = avcodec_parameters_to_context(dec_ctx, stream->codecpar);
        if (ret < 0) {
            loge("Failed to copy decoder parameters to input decoder context "
                         "for stream #%u\n", i);
            return ret;
        }

        av_codec_set_pkt_timebase(dec_ctx, stream->time_base);

        print_AVRational(dec_ctx->time_base, "open_input_file dec_ctx->time_base");
        print_AVRational(stream->time_base, "open_input_file in stream->time_base");

        codec = avcodec_find_decoder(dec_ctx->codec_id);

        if (!codec) {
            loge("Necessary decoder not found\n");
            return AVERROR_DECODER_NOT_FOUND;
        } else {
            logd("find decoder : %s \n", codec->name);
        }
        dec_ctx->codec_id = codec->id;





        if (dec_ctx->codec_type == AVMEDIA_TYPE_AUDIO) {
            /* 打开解码器 */
            ret = avcodec_open2(dec_ctx, codec, NULL);
            if (ret < 0) {
                loge("Failed to open decoder for stream #%u\n", i);
                return ret;
            }


            audio_stream_index = i;
            audio_dec_ctx = dec_ctx;
            sample_fmt = audio_dec_ctx->sample_fmt;

            print_AVRational(dec_ctx->time_base, "avcodec_open2 dec_ctx->time_base");
            print_AVRational(stream->time_base, "avcodec_open2 in stream->time_base");

        }
    }
    av_dump_format(ifmt_ctx, 0, filename, 0);

    print_audio_codecCtx_tag(audio_dec_ctx, "背景音乐解码器");

    return 0;
}


void ffmusic_decode_release() {

}