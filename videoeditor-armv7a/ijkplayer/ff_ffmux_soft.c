//
// Created by wyh3 on 2018/3/26.
//

#include "ff_ffmux_soft.h"
#include "ff_ffmux.h"

static AVFormatContext *in_fmt_ctx;
static AVCodecContext *video_dec_ctx;
static AVCodecContext *audio_dec_ctx;

static AVFormatContext *out_fmt_ctx;
static AVCodecContext *video_enc_ctx;
static AVCodecContext *audio_enc_ctx;

const int ffmux_default_output_bitrate = 2000001;

static int ffmux_open_output_file(EditorState *es) {
    AVStream *out_stream;
    AVStream *in_stream;
    AVCodecContext *dec_ctx, *enc_ctx;
    AVCodec *encoder;
    AVDictionary *encode_opts = NULL;
    int ret;
    unsigned int i;
    const char *filename;
    filename = es->outputPath;
    out_fmt_ctx = NULL;

    avformat_alloc_output_context2(&out_fmt_ctx, NULL, NULL, filename);
    if (!out_fmt_ctx) {
        loge("Could not create output context\n");
        return AVERROR_UNKNOWN;
    }
    logd("video src : start_time=%lld  ; output : duration=%lld ",
         in_fmt_ctx->start_time, in_fmt_ctx->duration);

    //分别初始化各个流
    for (i = 0; i < in_fmt_ctx->nb_streams; i++) {
        out_stream = avformat_new_stream(out_fmt_ctx, NULL);
        if (!out_stream) {
            loge("Failed allocating output stream\n");
            return AVERROR_UNKNOWN;
        }
        in_stream = in_fmt_ctx->streams[i];
//        dec_ctx = stream_ctx[i].dec_ctx;

        if (dec_ctx->codec_type == AVMEDIA_TYPE_VIDEO
            || dec_ctx->codec_type == AVMEDIA_TYPE_AUDIO) {
            //获取编码器
            encoder = avcodec_find_encoder(dec_ctx->codec_id);
            if (!encoder) {
                loge("Necessary encoder not found\n");
                return AVERROR_INVALIDDATA;
            } else {
                logd("find encoder : %s \n", encoder->name);
            }
            //编码器上下文
            enc_ctx = avcodec_alloc_context3(encoder);
            if (!enc_ctx) {
                loge("Failed to allocate the encoder context\n");
                return AVERROR(ENOMEM);
            }

            if (dec_ctx->codec_type == AVMEDIA_TYPE_VIDEO) {

                es->outputWidth = dec_ctx->width;
                es->outputHeight = dec_ctx->height;

                enc_ctx->width = es->outputWidth;
                enc_ctx->height = es->outputHeight;
                logd("video src : w=%d,h=%d ; output : w=%d,h=%d",
                     dec_ctx->width, dec_ctx->height, enc_ctx->width, enc_ctx->height);

                //设置角度，若不旋转frame，则需保留角度
                if ((ret = av_dict_set_int(&out_stream->metadata, IJKM_KEY_ROTATE,
                                           (int64_t) es->rotation, 0)) < 0) {
                    loge("Failed to set metadata rotation=%f", es->rotation);
                    return ret;
                } else {
                    logd("set metadata rotation=%f", es->rotation);
                }

                /* h264编码器特有设置域，具体见“ijk记录” */
                av_opt_set(enc_ctx->priv_data, "preset", "ultrafast", 0);
                av_opt_set(enc_ctx->priv_data, "lookahead", "0", 0);
//                av_opt_set(enc_ctx->priv_data, "level", "4.1", 0);
                av_opt_set(enc_ctx->priv_data, "2pass", "0", 0);
                av_opt_set(enc_ctx->priv_data, "zerolatency", "1", 0);
//                av_dict_set(&encode_opts, "profile", "baseline", 0);

                //设置码率，只设置bit_rate是平均码率，不一定能控制住
                if (!es->outputBitrate) {
                    logd("未手动设置输出码率，默认设置为%d ", ffmux_default_output_bitrate);
                    es->outputBitrate = ffmux_default_output_bitrate;
                }

                enc_ctx->bit_rate = es->outputBitrate;
                enc_ctx->rc_max_rate = es->outputBitrate;
                enc_ctx->rc_min_rate = es->outputBitrate;
                logd("video src : bit_rate=%lld  ; output : bit_rate=%lld ",
                     dec_ctx->bit_rate, enc_ctx->bit_rate);

                //设置宽高比
                enc_ctx->sample_aspect_ratio = dec_ctx->sample_aspect_ratio;

                //设置帧格式
                /* take first format from list of supported formats */
                if (encoder->pix_fmts)
                    enc_ctx->pix_fmt = encoder->pix_fmts[0];
                else
                    enc_ctx->pix_fmt = dec_ctx->pix_fmt;

                es->pix_fmt = enc_ctx->pix_fmt;

                //设置时间基准
                enc_ctx->time_base = dec_ctx->time_base;

                //必须设置，否则系统播放器及pc无法播放(ijk可以)
                enc_ctx->flags = AV_CODEC_FLAG_GLOBAL_HEADER;

            } else {

                logd("audio src sample_rate=%d ; output sample_rate=%d",
                     dec_ctx->sample_rate, dec_ctx->sample_rate);
                logd("audio src channel_layout=%lld ; output channel_layout=%lld",
                     dec_ctx->channel_layout, dec_ctx->channel_layout);
                logd("audio src channels=%d ; output channels=%d",
                     dec_ctx->channels, dec_ctx->channels);
                logd("audio src sample_fmt=%d ; output sample_fmt=%d",
                     dec_ctx->sample_fmt, dec_ctx->sample_fmt);
                logd("audio src time_base=%d ; output time_base=%d",
                     dec_ctx->time_base, dec_ctx->time_base);

                enc_ctx->sample_rate = dec_ctx->sample_rate;
                enc_ctx->channel_layout = dec_ctx->channel_layout;
                enc_ctx->channels = av_get_channel_layout_nb_channels(enc_ctx->channel_layout);
                /* take first format from list of supported formats */
                enc_ctx->sample_fmt = encoder->sample_fmts[0];
                enc_ctx->time_base = (AVRational) {1, enc_ctx->sample_rate};
            }

            /* Third parameter can be used to pass settings to encoder */
            ret = avcodec_open2(enc_ctx, encoder, &encode_opts);
            if (ret < 0) {
                loge("Cannot open video encoder for stream #%u\n", i);
                return ret;
            }
            ret = avcodec_parameters_from_context(out_stream->codecpar, enc_ctx);
            if (ret < 0) {
                loge("Failed to copy encoder parameters to output stream #%u\n", i);
                return ret;
            }
            if (out_fmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
                enc_ctx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

            out_stream->time_base = enc_ctx->time_base;
//            stream_ctx[i].enc_ctx = enc_ctx;
        } else if (dec_ctx->codec_type == AVMEDIA_TYPE_UNKNOWN) {
            loge("Elementary stream #%d is of unknown type, cannot proceed\n", i);
            return AVERROR_INVALIDDATA;
        } else {
            /* if this stream must be remuxed */
            ret = avcodec_parameters_copy(out_stream->codecpar, in_stream->codecpar);
            if (ret < 0) {
                loge("Copying parameters for stream #%u failed\n", i);
                return ret;
            }
            out_stream->time_base = in_stream->time_base;
        }

    }
    av_dump_format(out_fmt_ctx, 0, filename, 1);

    if (!(out_fmt_ctx->oformat->flags & AVFMT_NOFILE)) {
        ret = avio_open(&out_fmt_ctx->pb, filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            loge("Could not open output file '%s'", filename);
            return ret;
        }
    }

    /* init muxer, write output file header */
    ret = avformat_write_header(out_fmt_ctx, NULL);
    if (ret < 0) {
        loge("Error occurred when avformat_write_header\n");
        return ret;
    }

    return 0;
}

void init_soft(FFPlayer *ffp) {
    if (!ffp) {
        loge("init_soft !ffp");
        return;
    }
    VideoState *is = ffp->is;
    if (!is) {
        loge("init_soft !is");
        return;
    }
    if (is->ic) {
        in_fmt_ctx = is->ic;
    } else {
        loge("init_soft !is->ic");
        return;
    }
    if (is->viddec.avctx) {
        logd("init_soft 记录 video_dec_ctx");
        video_dec_ctx = is->viddec.avctx;
    }

    if (is->auddec.avctx) {
        logd("init_soft 记录 audio_dec_ctx");
        audio_dec_ctx = is->auddec.avctx;
    }
    EditorState *es = ffp->es;
    ffmux_open_output_file(es);
}

void release_soft() {

}

void video_encode_soft(AVFrame *frame) {


}

void audio_encode_soft(AVFrame *frame) {

}


