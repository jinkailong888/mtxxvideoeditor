//

// Created by wyh3 on 2018/3/26.
//

#include "ff_ffmux_soft.h"

static AVFormatContext *in_fmt_ctx;
static AVCodecContext *video_dec_ctx;
static AVCodecContext *audio_dec_ctx;
static AVFormatContext *out_fmt_ctx;
static AVCodecContext *video_enc_ctx;
static AVCodecContext *audio_enc_ctx;
static int video_stream_index = -1;
static int audio_stream_index = -1;
static const int ffmux_default_output_bitrate = 2000001;
static int64_t ffmux_soft_static_pts;
static bool ffmux_soft_video_encode_done;
static bool ffmux_soft_audio_encode_done;
static bool ffmux_soft_init;
static const bool ffmux_soft_ignoreAudio = true;

int ff_ffmux_soft_flush_video();

#define av_err2str(errnum) \
    av_make_error_string((char[AV_ERROR_MAX_STRING_SIZE]){0}, AV_ERROR_MAX_STRING_SIZE, errnum)


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

        //设置时间基准
        video_enc_ctx->time_base = video_dec_ctx->time_base;
        //av_codec_set_pkt_timebase方法未成功设置解码器的 timebase
        video_enc_ctx->time_base.den = 30;
        video_enc_ctx->time_base.num = 1;
        video_enc_ctx->flags = AV_CODEC_FLAG_GLOBAL_HEADER;
        logd("pix_fmt %d", video_enc_ctx->pix_fmt);
        logd("time_base den=%d num=%d", video_enc_ctx->time_base.den, video_enc_ctx->time_base.num);

        // 返回-22 ,原因为   video_enc_ctx->time_base 字段参数无效
        video_enc_ctx->codec_type = encoder->type;
        video_enc_ctx->gop_size = 30;
        video_enc_ctx->keyint_min = 60;
        video_enc_ctx->framerate.num = 1;
        video_enc_ctx->framerate.den = 30;
        video_enc_ctx->time_base.num = 1;
        video_enc_ctx->time_base.den = 30;

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
        out_stream->time_base.num = 1;
        out_stream->time_base.den = 90000;


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
        audio_enc_ctx->channels = av_get_channel_layout_nb_channels(audio_dec_ctx->channel_layout);
        /* take first format from list of supported formats */
        audio_enc_ctx->sample_fmt = encoder->sample_fmts[0];
        audio_enc_ctx->time_base = (AVRational) {1, audio_dec_ctx->sample_rate};


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
    if (!ffmux_soft_ignoreAudio)
        audio_dec_ctx = p_audio_dec_ctx;
    ffmux_open_output_file(es);
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
        ff_ffmux_soft_flush_video();
        ff_ffmux_soft_close();
    }
}

int ff_ffmux_soft_flush_video() {
    int got_frame;
    int ret;
    if (!(video_enc_ctx->codec->capabilities &
          AV_CODEC_CAP_DELAY))
        return 0;
    while (1) {
        logd("Flushing stream %s encoder\n", "video");
        ret = ff_ffmux_soft_onFrameEncode(NULL, &got_frame);
        if (ret < 0)
            break;
        if (!got_frame)
            return 0;
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

    avformat_close_input(&in_fmt_ctx);

    if (out_fmt_ctx && !(out_fmt_ctx->oformat->flags & AVFMT_NOFILE))
        avio_closep(&out_fmt_ctx->pb);
    avformat_free_context(out_fmt_ctx);

    logd("ff_ffmux_soft_release end \n");

}


void ff_ffmux_soft_onVideoEncode(unsigned char *data, double pts, int size, int width, int height) {
    if (!ffmux_soft_init) {
        return;
    }
    logd("ff_ffmux_soft_onVideoEncode pts=%f,size=%d,width=%d,height=%d", pts, size, width, height);

    AVFrame *pFrame = av_frame_alloc();
    int picture_size = avpicture_get_size(video_enc_ctx->pix_fmt,
                                          video_enc_ctx->width, video_enc_ctx->height);
    uint8_t* picture_buf = (uint8_t *)av_malloc((size_t) picture_size);
    avpicture_fill((AVPicture *)pFrame, picture_buf, video_enc_ctx->pix_fmt,
                   video_enc_ctx->width, video_enc_ctx->height);

}


int ff_ffmux_soft_onFrameEncode(AVFrame *frame, int *got_frame) {
    int ret = 0;
    if (!ffmux_soft_init) {
        return 0;
    }
    AVPacket encode_pkt;
    encode_pkt.data = NULL;
    encode_pkt.size = 0;
    av_init_packet(&encode_pkt);
//    int encode_pkt_size = av_image_get_buffer_size(
//            video_enc_ctx->pix_fmt, video_enc_ctx->width, video_enc_ctx->height, 1);
//
//    logd("encode_pkt_size=%d", encode_pkt_size);
//
//    ret = av_new_packet(&encode_pkt, encode_pkt_size);

//    if (ret < 0) {
//        loge("Failed to av_new_packet");
//        av_err2str(ret);
//    }

    if (frame) {
        if (frame->data) {
            logd("frame->data");
        } else {
            loge("!!!frame->data");
        }

        logd("format->width=%d", frame->width);
        logd("format->height=%d", frame->height);
        logd("frame->format=%d", frame->format);
        logd("frame->pts=%lld", frame->pts);
        logd("frame->pkt_dts=%lld", frame->pkt_dts);
        logd("frame->pkt_size=%d", frame->pkt_size);
        logd("frame->linesize[0]=%d", frame->linesize[0]);
        logd("frame->linesize[1]=%d", frame->linesize[1]);
        logd("frame->linesize[2]=%d", frame->linesize[2]);

//        frame->width = video_enc_ctx->width;
//        frame->height = video_enc_ctx->height;
//        frame->format = video_enc_ctx->pix_fmt;
//        frame->pts = (int64_t) ((1.0 / 30) * 90 * ffmux_soft_static_pts);
//        ffmux_soft_static_pts++;


//        uint8_t *ptrPictureBuf = (uint8_t *) av_malloc((size_t) encode_pkt_size);
//
//        av_image_fill_arrays(frame->data,
//                             frame->linesize,
//                             ptrPictureBuf,
//                             video_enc_ctx->pix_fmt,
//                             video_enc_ctx->width,
//                             video_enc_ctx->height,
//                             1);

//        encode_pkt.dts = frame->pts;
    }

    ret = avcodec_encode_video2(video_enc_ctx, &encode_pkt, frame, &got_frame);

    if (ret < 0) {
        loge("avcodec_encode_video2 Error ret = %d\n", ret);
        av_err2str(ret);
        goto encode_end;
    }
    logd("encode_pkt.pts=%lld",encode_pkt.pts);
    logd("encode_pkt.dts=%lld",encode_pkt.dts);
    encode_pkt.stream_index = video_stream_index;

    //编码后，pkt.pts、pkt.dts使用AVCodecContext->time_base为单位
    // 通过调用"av_packet_rescale_ts"转换为AVStream->time_base为单位
//    av_packet_rescale_ts(&encode_pkt,
//                         video_dec_ctx->time_base,
//                         out_fmt_ctx->streams[video_stream_index]->time_base
//                         );

    logd("rescale encode_pkt.pts=%lld",encode_pkt.pts);
    logd("rescale encode_pkt.dts=%lld",encode_pkt.dts);

    ret = av_write_frame(out_fmt_ctx, &encode_pkt);
    if (ret < 0) {
        loge("Failed to muxing frame ret=%d\n", ret);
        av_err2str(ret);
    }
    encode_end:
    av_packet_unref(&encode_pkt);
    return ret;
}


void ff_ffmux_soft_onAudioEncode(AVFrame *frame) {
    if (!ffmux_soft_init) {
        return;
    }
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

