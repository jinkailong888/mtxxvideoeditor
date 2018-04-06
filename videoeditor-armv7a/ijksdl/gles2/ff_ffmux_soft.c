//

// Created by wyh3 on 2018/3/26.
//

#define FFMUX_SOFT_CONFIG_FILTER 1

#include "ff_ffmux_soft.h"
#include "ff_print_util.h"
#include <libavfilter/avfiltergraph.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>

static const int VIDEO_TYPE = -1;
static const int AUDIO_TYPE = -2;
static AVFormatContext *in_fmt_ctx;
static AVCodecContext *video_dec_ctx;
static AVCodecContext *audio_dec_ctx;
static AVFormatContext *out_fmt_ctx;
static AVCodecContext *video_enc_ctx;
static AVCodecContext *audio_enc_ctx;
static int video_stream_index = -1;
static int audio_stream_index = -1;
static const int ffmux_default_output_bitrate = 2000001;
static bool ffmux_soft_video_encode_done;
static bool ffmux_soft_audio_encode_done;
static bool ffmux_soft_init;
static const bool ffmux_soft_ignoreAudio = false;
static const bool ffmux_soft_print = true;

static const enum AVPixelFormat ffmux_soft_gl_pix_fmt = AV_PIX_FMT_BGR24;

static struct SwsContext *ffmux_soft_frame_img_convert_ctx;


static int ff_ffmux_soft_onFrameEncode(AVFrame *frame, int *got_frame, const int type);

int ff_ffmux_flush_encode(const int type);

#define av_err2str(errnum) \
    av_make_error_string((char[AV_ERROR_MAX_STRING_SIZE]){0}, AV_ERROR_MAX_STRING_SIZE, errnum)


#if FFMUX_SOFT_CONFIG_FILTER
const char *ffmux_soft_video_filter_spec = "null";
const char *ffmux_soft_audio_filter_spec = "anull";

typedef struct FilteringContext {
    AVFilterContext *buffersink_ctx;
    AVFilterContext *buffersrc_ctx;
    AVFilterGraph *filter_graph;
} FilteringContext;
static FilteringContext *filter_ctx;

static int ff_ffmux_soft_init_filter(FilteringContext *fctx, AVCodecContext *dec_ctx,
                                     AVCodecContext *enc_ctx, const char *filter_spec) {
    char args[512];
    int ret = 0;
    AVFilter *buffersrc = NULL;
    AVFilter *buffersink = NULL;
    AVFilterContext *buffersrc_ctx = NULL;
    AVFilterContext *buffersink_ctx = NULL;
    AVFilterInOut *outputs = avfilter_inout_alloc();
    AVFilterInOut *inputs = avfilter_inout_alloc();
    AVFilterGraph *filter_graph = avfilter_graph_alloc();

    if (!outputs || !inputs || !filter_graph) {
        ret = AVERROR(ENOMEM);
        goto end;
    }

    if (dec_ctx->codec_type == AVMEDIA_TYPE_VIDEO) {
        buffersrc = avfilter_get_by_name("buffer");
        buffersink = avfilter_get_by_name("buffersink");
        if (!buffersrc || !buffersink) {
            loge("filtering source or sink element not found\n");
            ret = AVERROR_UNKNOWN;
            goto end;
        }

        snprintf(args, sizeof(args),
                 "video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
                 dec_ctx->width, dec_ctx->height, dec_ctx->pix_fmt,
                 dec_ctx->time_base.num, dec_ctx->time_base.den,
                 dec_ctx->sample_aspect_ratio.num,
                 dec_ctx->sample_aspect_ratio.den);

        ret = avfilter_graph_create_filter(&buffersrc_ctx, buffersrc, "in",
                                           args, NULL, filter_graph);
        if (ret < 0) {
            loge("Cannot create buffer source\n");
            goto end;
        }

        ret = avfilter_graph_create_filter(&buffersink_ctx, buffersink, "out",
                                           NULL, NULL, filter_graph);
        if (ret < 0) {
            loge("Cannot create buffer sink\n");
            goto end;
        }

        //通过avfilter做帧格式转换
        ret = av_opt_set_bin(buffersink_ctx, "pix_fmts",
                             (uint8_t *) &enc_ctx->pix_fmt, sizeof(enc_ctx->pix_fmt),
                             AV_OPT_SEARCH_CHILDREN);
        if (ret < 0) {
            loge("Cannot set output pixel format\n");
            goto end;
        }
    } else if (dec_ctx->codec_type == AVMEDIA_TYPE_AUDIO) {
        buffersrc = avfilter_get_by_name("abuffer");
        buffersink = avfilter_get_by_name("abuffersink");
        if (!buffersrc || !buffersink) {
            loge("filtering source or sink element not found\n");
            ret = AVERROR_UNKNOWN;
            goto end;
        }

        if (!dec_ctx->channel_layout)
            dec_ctx->channel_layout =
                    av_get_default_channel_layout(dec_ctx->channels);
        snprintf(args, sizeof(args),
                 "time_base=%d/%d:sample_rate=%d:sample_fmt=%s:channel_layout=0x%"PRIx64,
                 dec_ctx->time_base.num, dec_ctx->time_base.den, dec_ctx->sample_rate,
                 av_get_sample_fmt_name(dec_ctx->sample_fmt),
                 dec_ctx->channel_layout);
        ret = avfilter_graph_create_filter(&buffersrc_ctx, buffersrc, "in",
                                           args, NULL, filter_graph);
        if (ret < 0) {
            loge("Cannot create audio buffer source\n");
            goto end;
        }

        ret = avfilter_graph_create_filter(&buffersink_ctx, buffersink, "out",
                                           NULL, NULL, filter_graph);
        if (ret < 0) {
            loge("Cannot create audio buffer sink\n");
            goto end;
        }

        //借助avfilter做音频数据格式转换、重采样等操作
        ret = av_opt_set_bin(buffersink_ctx, "sample_fmts",
                             (uint8_t *) &enc_ctx->sample_fmt, sizeof(enc_ctx->sample_fmt),
                             AV_OPT_SEARCH_CHILDREN);
        if (ret < 0) {
            loge("Cannot set output sample format\n");
            goto end;
        }

        ret = av_opt_set_bin(buffersink_ctx, "channel_layouts",
                             (uint8_t *) &enc_ctx->channel_layout,
                             sizeof(enc_ctx->channel_layout), AV_OPT_SEARCH_CHILDREN);
        if (ret < 0) {
            loge("Cannot set output channel layout\n");
            goto end;
        }

        ret = av_opt_set_bin(buffersink_ctx, "sample_rates",
                             (uint8_t *) &enc_ctx->sample_rate, sizeof(enc_ctx->sample_rate),
                             AV_OPT_SEARCH_CHILDREN);
        if (ret < 0) {
            loge("Cannot set output sample rate\n");
            goto end;
        }
    } else {
        ret = AVERROR_UNKNOWN;
        goto end;
    }

    /* Endpoints for the filter graph. */
    outputs->name = av_strdup("in");
    outputs->filter_ctx = buffersrc_ctx;
    outputs->pad_idx = 0;
    outputs->next = NULL;

    inputs->name = av_strdup("out");
    inputs->filter_ctx = buffersink_ctx;
    inputs->pad_idx = 0;
    inputs->next = NULL;

    if (!outputs->name || !inputs->name) {
        ret = AVERROR(ENOMEM);
        goto end;
    }

    if ((ret = avfilter_graph_parse_ptr(filter_graph, filter_spec,
                                        &inputs, &outputs, NULL)) < 0)
        goto end;

    if ((ret = avfilter_graph_config(filter_graph, NULL)) < 0)
        goto end;

    /* Fill FilteringContext */
    fctx->buffersrc_ctx = buffersrc_ctx;
    fctx->buffersink_ctx = buffersink_ctx;
    fctx->filter_graph = filter_graph;

    end:
    avfilter_inout_free(&inputs);
    avfilter_inout_free(&outputs);

    return ret;
}

static int ff_ffmux_soft_init_filters(void) {
    const char *filter_spec;
    unsigned int i;
    int ret;
    AVCodecContext *dec_ctx, *enc_ctx;
    filter_ctx = av_malloc_array(in_fmt_ctx->nb_streams, sizeof(*filter_ctx));
    if (!filter_ctx)
        return AVERROR(ENOMEM);

    for (i = 0; i < in_fmt_ctx->nb_streams; i++) {
        filter_ctx[i].buffersrc_ctx = NULL;
        filter_ctx[i].buffersink_ctx = NULL;
        filter_ctx[i].filter_graph = NULL;
        if (!(in_fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO
              || in_fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO))
            continue;
        if (in_fmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            dec_ctx = video_dec_ctx;
            enc_ctx = video_enc_ctx;
            filter_spec = ffmux_soft_video_filter_spec;
        } else {
            dec_ctx = audio_dec_ctx;
            enc_ctx = audio_enc_ctx;
            filter_spec = ffmux_soft_audio_filter_spec;
        }

        ret = ff_ffmux_soft_init_filter(&filter_ctx[i], dec_ctx, enc_ctx, filter_spec);
        if (ret)
            return ret;
    }
    return 0;
}

static int ff_ffmux_soft_filter_encode_write_frame(AVFrame *frame, const int type) {
    int ret;
    AVFrame *filt_frame;
    int stream_index = type == VIDEO_TYPE ? video_stream_index : audio_stream_index;
    logd("Pushing decoded frame to filters\n");

    /* push the decoded frame into the filtergraph */
    //todo Changing frame properties on the fly is not supported by all filters
    ret = av_buffersrc_add_frame_flags(filter_ctx[stream_index].buffersrc_ctx,
                                       frame, 0);
    if (ret < 0) {
        loge("Error while feeding the filtergraph ret=%d \n", ret);
        av_err2str(ret);
        return ret;
    }

    /* pull filtered frames from the filtergraph */
    while (1) {
        filt_frame = av_frame_alloc();
        if (!filt_frame) {
            ret = AVERROR(ENOMEM);
            break;
        }
        logd("Pulling filtered frame from filters\n");
        ret = av_buffersink_get_frame(filter_ctx[stream_index].buffersink_ctx,
                                      filt_frame);
        if (ret < 0) {
            /* if no more frames for output - returns AVERROR(EAGAIN)
             * if flushed and no more frames for output - returns AVERROR_EOF
             * rewrite retcode to 0 to show it as normal procedure completion
             */
            if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF)
                ret = 0;
            av_frame_free(&filt_frame);
            break;
        }
        filt_frame->pict_type = AV_PICTURE_TYPE_NONE;
        ret = ff_ffmux_soft_onFrameEncode(filt_frame, NULL, type);
        if (ret < 0)
            break;
    }

    return ret;
}

#endif

static int ff_ffmux_soft_onFrameEncode(AVFrame *frame, int *got_frame, const int type) {
    if (!ffmux_soft_init) {
        loge("软解软保未初始化！");
        return 0;
    }
    AVCodecContext *enc_ctx;
    int (*enc_func)(AVCodecContext *, AVPacket *, const AVFrame *, int *);
    int stream_index;

    bool is_video_type = type == VIDEO_TYPE ? true : false;

    if (is_video_type) {
        enc_ctx = video_enc_ctx;
        enc_func = avcodec_encode_video2;
        stream_index = video_stream_index;
    } else {
        enc_ctx = audio_enc_ctx;
        enc_func = avcodec_encode_audio2;
        stream_index = audio_stream_index;
    }

    int ret;
    int got_frame_local;
    if (!got_frame)
        got_frame = &got_frame_local;

    AVPacket encode_pkt;
    encode_pkt.data = NULL;
    encode_pkt.size = 0;
    av_init_packet(&encode_pkt);

    if (ffmux_soft_print) {
        if (is_video_type) {
            print_avframe_tag(frame, "即将编码-----视频帧----");
        } else {
//            print_avframe_tag(frame, "即将编码-----音频帧---");
        }
    }

    ret = enc_func(enc_ctx, &encode_pkt, frame, got_frame);

    if (ret < 0) {
        loge("enc_func Error ret = %d\n", ret);
        av_err2str(ret);
        return ret;
    }

    if (!(*got_frame))
        return 0;

    if (ffmux_soft_print) {
        if (is_video_type) {
            print_avpacket_tag(&encode_pkt, "编码后 视频 包");
            print_AVRational(enc_ctx->time_base, "编码后视频包开始转换时间基 编码器为:");
            print_AVRational(out_fmt_ctx->streams[stream_index]->time_base,
                             "编码后视频包开始转换时间基 视频输出流为:");
        } else {
//            print_avpacket_tag(&encode_pkt, "编码后 音频 包");
//            print_AVRational(enc_ctx->time_base, "编码后音频包开始转换时间基 编码器为:");
//            print_AVRational(out_fmt_ctx->streams[stream_index]->time_base,
//                             "编码后音频包开始转换时间基 音频输出流为:");
        }
    }

    //准备mux
    encode_pkt.stream_index = stream_index;
    av_packet_rescale_ts(&encode_pkt,
                         enc_ctx->time_base,
                         out_fmt_ctx->streams[stream_index]->time_base
    );

    if (ffmux_soft_print) {
        if (is_video_type) {
            print_avpacket_tag(&encode_pkt, "即将写入的 视频 包");
        } else {
//            print_avpacket_tag(&encode_pkt, "即将写入的 音频 包");
        }
    }

    ret = av_write_frame(out_fmt_ctx, &encode_pkt);
    if (ret < 0) {
        if (is_video_type) {
            loge("Failed to muxing 视频 frame ret=%d\n", ret);
        } else {
//            loge("Failed to muxing 音频 frame ret=%d\n", ret);
        }
        av_err2str(ret);
    } else {
        if (is_video_type) {
            logd("成功写入 视频 包");
        } else {
//            logd("成功写入 音频 包");
        }
    }
    return ret;
}

static int ffmux_open_output_file(EditorState *es) {
    AVStream *out_stream;
    AVCodec *encoder;
    int ret;
    const char *filename;
    filename = es->outputPath;
    out_fmt_ctx = NULL;

    logd("ffmux_open_output_file filename:%s", es->outputPath);

    avformat_alloc_output_context2(&out_fmt_ctx, NULL, NULL, filename);
    if (!out_fmt_ctx) {
        loge("Could not create output context\n");
        return AVERROR_UNKNOWN;
    }
    //初始化视频流
    if (video_dec_ctx) {
        logd("开始初始化视频编码器");
        out_stream = avformat_new_stream(out_fmt_ctx, NULL);
        if (!out_stream) {
            loge("Failed allocating output stream\n");
            return AVERROR_UNKNOWN;
        }
        video_stream_index = out_stream->index;
        encoder = avcodec_find_encoder(video_dec_ctx->codec_id);
        if (!encoder) {
            loge("Necessary video encoder not found, codeId=%d\n", video_dec_ctx->codec_id);
            return AVERROR_INVALIDDATA;
        } else {
            logd("find video encoder : %s \n", encoder->name);
        }
        video_enc_ctx = avcodec_alloc_context3(encoder);
        if (!video_enc_ctx) {
            loge("Failed to allocate the encoder video_enc_ctx\n");
            return AVERROR(ENOMEM);
        }
        //宽高
        es->outputWidth = video_dec_ctx->width;
        es->outputHeight = video_dec_ctx->height;

        video_enc_ctx->width = es->outputWidth;
        video_enc_ctx->height = es->outputHeight;
        logd("video src : w=%d,h=%d ; output : w=%d,h=%d",
             video_dec_ctx->width, video_dec_ctx->height,
             video_enc_ctx->width, video_enc_ctx->height);

        //设置角度，若不旋转frame，则需保留角度
        if ((ret = av_dict_set_int(&out_stream->metadata, "rotate",
                                   (int64_t) es->rotation, 0)) < 0) {
            loge("Failed to set metadata rotation=%f", es->rotation);
            return ret;
        } else {
            logd("set metadata rotation=%f", es->rotation);
        }

        /* h264编码器特有设置域，具体见“ijk记录” */
        av_opt_set(video_enc_ctx->priv_data, "preset", "ultrafast", 0);
        av_opt_set(video_enc_ctx->priv_data, "lookahead", "0", 0);
        av_opt_set(video_enc_ctx->priv_data, "2pass", "0", 0);
        av_opt_set(video_enc_ctx->priv_data, "zerolatency", "1", 0);

        //设置码率，只设置bit_rate是平均码率，不一定能控制住
        if (!es->outputBitrate) {
            logd("未手动设置输出码率，默认设置为%d ", ffmux_default_output_bitrate);
            es->outputBitrate = ffmux_default_output_bitrate;
        }
        video_enc_ctx->bit_rate = es->outputBitrate;
        video_enc_ctx->rc_max_rate = es->outputBitrate;
        video_enc_ctx->rc_min_rate = es->outputBitrate;
        logd("video src : bit_rate=%lld  ; output : bit_rate=%lld ",
             video_dec_ctx->bit_rate, video_enc_ctx->bit_rate);
        //设置宽高比
        video_enc_ctx->sample_aspect_ratio = video_dec_ctx->sample_aspect_ratio;
        //设置帧格式
        /* take first format from list of supported formats */
        if (encoder->pix_fmts)
            video_enc_ctx->pix_fmt = encoder->pix_fmts[0];
        else
            video_enc_ctx->pix_fmt = video_dec_ctx->pix_fmt;
        es->pix_fmt = video_enc_ctx->pix_fmt;


        logd(" pix_fmt  decoder:%s encodec:%s",
             av_pix_fmt_desc_get(video_dec_ctx->pix_fmt)->name,
             av_pix_fmt_desc_get(video_enc_ctx->pix_fmt)->name);

        video_enc_ctx->codec_type = encoder->type;
        video_enc_ctx->gop_size = 30;
        video_enc_ctx->keyint_min = 60;
        video_enc_ctx->time_base = video_dec_ctx->time_base;
        video_enc_ctx->framerate = video_dec_ctx->framerate;
        video_enc_ctx->flags = AV_CODEC_FLAG_GLOBAL_HEADER;

        AVDictionary *opts = NULL;
        av_dict_set(&opts, "threads", "auto", 0);
        av_dict_set(&opts, "profile", "main", 0);
        ret = avcodec_open2(video_enc_ctx, encoder, &opts);
        if (ret < 0) {
            loge("Cannot open video encoder ret=%d", ret);
            return ret;
        }
        ret = avcodec_parameters_from_context(out_stream->codecpar, video_enc_ctx);
        if (ret < 0) {
            loge("Failed to copy video encoder parameters");
            return ret;
        }
        out_stream->time_base = video_enc_ctx->time_base;
        logd("视频编码器初始化完毕");
    }

    //初始化音频流
    if (audio_dec_ctx) {

        logd("开始初始化音频编码器");

        out_stream = avformat_new_stream(out_fmt_ctx, NULL);
        if (!out_stream) {
            loge("Failed allocating audio output stream\n");
            return AVERROR_UNKNOWN;
        }
        encoder = avcodec_find_encoder(audio_dec_ctx->codec_id);
        if (!encoder) {
            loge("Necessary audio encoder not found\n");
            return AVERROR_INVALIDDATA;
        } else {
            logd("find audio encoder : %s \n", encoder->name);
        }
        audio_enc_ctx = avcodec_alloc_context3(encoder);
        if (!audio_enc_ctx) {
            loge("Failed to allocate the encoder audio_enc_ctx\n");
            return AVERROR(ENOMEM);
        }
        logd("audio src sample_rate=%d ; output sample_rate=%d",
             audio_dec_ctx->sample_rate, audio_dec_ctx->sample_rate);
        logd("audio src channel_layout=%lld ; output channel_layout=%lld",
             audio_dec_ctx->channel_layout, audio_dec_ctx->channel_layout);
        logd("audio src channels=%d ; output channels=%d",
             audio_dec_ctx->channels, audio_dec_ctx->channels);
        logd("audio src sample_fmt=%d ; output sample_fmt=%d",
             audio_dec_ctx->sample_fmt, audio_dec_ctx->sample_fmt);
        logd("audio src time_base=%d ; output time_base=%d",
             audio_dec_ctx->time_base, audio_dec_ctx->time_base);

        audio_enc_ctx->sample_rate = audio_dec_ctx->sample_rate;
        audio_enc_ctx->channel_layout = audio_dec_ctx->channel_layout;
        audio_enc_ctx->channels = av_get_channel_layout_nb_channels(audio_enc_ctx->channel_layout);
        /* take first format from list of supported formats */
        audio_enc_ctx->sample_fmt = encoder->sample_fmts[0];
//        audio_enc_ctx->time_base = (AVRational) {1, audio_dec_ctx->sample_rate};
//
        audio_enc_ctx->time_base = audio_dec_ctx->time_base;

        ret = avcodec_open2(audio_enc_ctx, encoder, NULL);

        if (ret < 0) {
            loge("Cannot open audio encoder");
            return ret;
        }
        ret = avcodec_parameters_from_context(out_stream->codecpar, audio_enc_ctx);
        if (ret < 0) {
            loge("Failed to copy audio encoder parameters");
            return ret;
        }
        if (out_fmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
            audio_enc_ctx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;


        out_stream->time_base = audio_enc_ctx->time_base;


        logd("音频编码器初始化完毕");
    }

    av_dump_format(out_fmt_ctx, 0, filename, 1);

    if (!(out_fmt_ctx->oformat->flags & AVFMT_NOFILE)) {
        ret = avio_open(&out_fmt_ctx->pb, filename, AVIO_FLAG_READ_WRITE);
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

void ff_ffmux_soft_init(AVFormatContext *p_in_fmt_ctx, AVCodecContext *p_video_dec_ctx,
                        AVCodecContext *p_audio_dec_ctx, EditorState *es) {
    logd("ff_ffmux_soft_init");
    ffmux_soft_init = true;
    in_fmt_ctx = p_in_fmt_ctx;

    int i;
    for (i = 0; i < in_fmt_ctx->nb_streams; i++) {
        AVStream *stream = in_fmt_ctx->streams[i];
        if (stream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream_index = i;
        }
        if (stream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_stream_index = i;
        }
    }

    video_dec_ctx = p_video_dec_ctx;
    audio_dec_ctx = p_audio_dec_ctx;
    ffmux_open_output_file(es);

#if FFMUX_SOFT_CONFIG_FILTER
    if (ff_ffmux_soft_init_filters() < 0) {
        loge("Failed to init filters");
    }
#endif
}

void ff_ffmux_soft_close() {
    int ret = av_write_trailer(out_fmt_ctx);
    if (ret != 0) {
        loge("av_write_trailer error !");
    }

    ff_ffmux_soft_release();
}

void ff_ffmux_soft_check_close() {

    ffmux_soft_audio_encode_done = ffmux_soft_ignoreAudio ? true : ffmux_soft_audio_encode_done;

    if (ffmux_soft_video_encode_done && ffmux_soft_audio_encode_done) {

        ff_ffmux_flush_encode(VIDEO_TYPE);
        ff_ffmux_flush_encode(AUDIO_TYPE);

        ff_ffmux_soft_close();
    }
}

int ff_ffmux_flush_encode(const int type) {
    int got_frame;
    int ret;
    char *tag = type == VIDEO_TYPE ? " 视频 " : " 音频 ";

    ret = ff_ffmux_soft_filter_encode_write_frame(NULL, type);
    if (ret < 0) {
        loge("Flushing filter failed\n");
        return ret;
    }

    AVCodecContext *enc_ctx = type == VIDEO_TYPE ? video_enc_ctx : audio_enc_ctx;

    if (!(enc_ctx->codec->capabilities &
          AV_CODEC_CAP_DELAY))
        return 0;
    while (1) {
        logd("Flushing stream %s encoder\n", tag);
        ret = ff_ffmux_soft_onFrameEncode(NULL, &got_frame, type);
        if (ret < 0)
            break;
        if (!got_frame) {
            logd("Flushing %s done", tag);
            return 0;
        }
    }
    return ret;
}


void ff_ffmux_soft_release() {

    logd("ff_ffmux_soft_release start \n");

    ffmux_soft_init = false;

    avcodec_free_context(&video_dec_ctx);
    avcodec_free_context(&audio_dec_ctx);
    avcodec_free_context(&video_enc_ctx);
    avcodec_free_context(&audio_enc_ctx);

#if FFMUX_SOFT_CONFIG_FILTER
    if (filter_ctx && filter_ctx[video_stream_index].filter_graph)
        avfilter_graph_free(&filter_ctx[video_stream_index].filter_graph);
    if (filter_ctx && filter_ctx[audio_stream_index].filter_graph)
        avfilter_graph_free(&filter_ctx[audio_stream_index].filter_graph);
    av_free(filter_ctx);
#endif

    avformat_close_input(&in_fmt_ctx);

    if (out_fmt_ctx && !(out_fmt_ctx->oformat->flags & AVFMT_NOFILE))
        avio_closep(&out_fmt_ctx->pb);
    avformat_free_context(out_fmt_ctx);

    if (ffmux_soft_frame_img_convert_ctx) {
        sws_freeContext(ffmux_soft_frame_img_convert_ctx);
    }

    logd("ff_ffmux_soft_release end \n");

}


void
ff_ffmux_soft_onVideoEncode(unsigned char *rgbaData, int64_t pts, int64_t dts, int format, int size,
                            int width, int height, AVDictionary *pDictionary,
                            enum AVColorRange range, enum AVColorPrimaries primaries,
                            enum AVColorTransferCharacteristic characteristic,
                            enum AVColorSpace space, enum AVChromaLocation location, int i1,
                            int64_t i2) {
    int ret;
    if (!ffmux_soft_init) {
        return;
    }

    AVFrame *pRgbaFrame = av_frame_alloc();


    ret = av_image_fill_arrays(pRgbaFrame->data,
                               pRgbaFrame->linesize,
//                               rgbaData,
                               (uint8_t *) av_malloc(size * sizeof(uint8_t)),
                               ffmux_soft_gl_pix_fmt,
                               width,
                               height,
                               1);

    if (ret < 0) {
        loge("%s pRgbaFrame av_image_fill_arrays failed\n", __func__);
        return;
    }
    pRgbaFrame->data[0] = rgbaData;


    AVFrame *pYuvFrame = av_frame_alloc();

    int bytes = av_image_get_buffer_size(video_dec_ctx->pix_fmt, width, height, 1);
    uint8_t *buffer = (uint8_t *) av_malloc(bytes * sizeof(uint8_t));
    if (!buffer) {
        loge("%s av_image_get_buffer_size failed\n", __func__);
        return;
    }


    ret = av_image_fill_arrays(pYuvFrame->data,
                               pYuvFrame->linesize,
                               buffer,
                               video_dec_ctx->pix_fmt,
                               width,
                               height,
                               1);
    if (ret < 0) {
        loge("%s pYuvFrame av_image_fill_arrays failed\n", __func__);
        return;
    }

    if (!ffmux_soft_frame_img_convert_ctx) {
        ffmux_soft_frame_img_convert_ctx = sws_getContext(width,
                                                          height,
                                                          ffmux_soft_gl_pix_fmt,
                                                          width,
                                                          height,
                                                          video_dec_ctx->pix_fmt,
                                                          SWS_BICUBIC,
                                                          NULL,
                                                          NULL,
                                                          NULL);
    }

    ret = sws_scale(ffmux_soft_frame_img_convert_ctx,
                    (const uint8_t *const *) pRgbaFrame->data,
                    pRgbaFrame->linesize,
                    0,
                    pRgbaFrame->height,
                    pYuvFrame->data,
                    pYuvFrame->linesize);

    if (ret < 0) {
        loge("%s sws_scale failed ret=%d\n", __func__, ret);
        av_err2str(ret);
        return;
    }

    pYuvFrame->width = width;
    pYuvFrame->height = height;
    pYuvFrame->pts = pts;
    pYuvFrame->pkt_dts = dts;
    pYuvFrame->pkt_size = i1;
    pYuvFrame->pkt_duration = i2;
    pYuvFrame->format = format;
    pYuvFrame->metadata = pDictionary;
    pYuvFrame->color_range = range;
    pYuvFrame->color_primaries = primaries;
    pYuvFrame->color_trc = characteristic;
    pYuvFrame->colorspace = space;
    pYuvFrame->chroma_location = location;



    print_avframe_tag(pYuvFrame, "sws_scale 转换后的视频帧：");



    ff_ffmux_soft_filter_encode_write_frame(pYuvFrame, VIDEO_TYPE);
}


int ff_ffmux_soft_onVideoFrameEncode(AVFrame *frame) {
#if FFMUX_SOFT_CONFIG_FILTER
    return ff_ffmux_soft_filter_encode_write_frame(frame, VIDEO_TYPE);
#endif
    return ff_ffmux_soft_onFrameEncode(frame, NULL, VIDEO_TYPE);
}

int ff_ffmux_soft_onAudioEncode(AVFrame *frame, int *got_frame) {
    if (ffmux_soft_ignoreAudio)
        return 0;
#if FFMUX_SOFT_CONFIG_FILTER
    return ff_ffmux_soft_filter_encode_write_frame(frame, AUDIO_TYPE);
#endif
    return ff_ffmux_soft_onFrameEncode(frame, NULL, AUDIO_TYPE);
}

void ff_ffmux_soft_onVideoEncodeDone() {
    if (!ffmux_soft_init) {
        return;
    }
    logd("ff_ffmux_soft_onVideoEncodeDone");
    ffmux_soft_video_encode_done = true;
    ff_ffmux_soft_check_close();
}


void ff_ffmux_soft_onAudioEncodeDone() {
    if (!ffmux_soft_init) {
        return;
    }
    ffmux_soft_audio_encode_done = true;
    ff_ffmux_soft_check_close();
}

