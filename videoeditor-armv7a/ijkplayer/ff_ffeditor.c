//
// **** MeiTu 视频保存 ****/
// Created by wyh3 on 2018/3/6.
//


#define CONFIG_FILTER 1
#define MY_TAG  "VideoEditor"

#define loge(format, ...)  __android_log_print(ANDROID_LOG_ERROR, MY_TAG, format, ##__VA_ARGS__)
#define logd(format, ...)  __android_log_print(ANDROID_LOG_DEBUG,  MY_TAG, format, ##__VA_ARGS__)

#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavfilter/avfiltergraph.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavutil/opt.h>
#include <libavutil/pixdesc.h>
#include <jni.h>
#include <android/gl_jni.h>
#include <android/gl_util.h>
#include "ff_ffplay_def.h"
#include "ff_cmdutils.h"
#include "ff_ffeditor.h"
#include "ff_mediacodec.h"

const int ffeditor_default_output_bitrate = 2000001;
const char *ffeditor_hd_video_codec_name = "h264_mediacodec";
//const char *ffeditor_hd_video_codec_name = "null";


//加水印耗时与不加差不多，但改变色调耗时巨长，由4S涨到20+S
//设置rgba四个分量的变换关系，共接受16个参数, 灰阶效果
//const char *ffeditor_video_filter_spec = "colorchannelmixer=.3:.4:.3:0:.3:.4:.3:0:.3:.4:.3";
//const char *ffeditor_video_filter_spec = "movie='/storage/emulated/0/VideoEditorDir/save.png',"
//        "scale=200:200[wm];[in][wm]overlay=W-w-5:H-h-5[out]";
const char *ffeditor_video_filter_spec = "null";
const char *ffeditor_audio_filter_spec = "anull";


static AVFormatContext *ifmt_ctx;
static AVFormatContext *ofmt_ctx;

#if CONFIG_FILTER
typedef struct FilteringContext {
    AVFilterContext *buffersink_ctx;
    AVFilterContext *buffersrc_ctx;
    AVFilterGraph *filter_graph;
} FilteringContext;

static FilteringContext *filter_ctx;
#endif

typedef struct StreamContext {
    AVCodecContext *dec_ctx;
    AVCodecContext *enc_ctx;
} StreamContext;
static StreamContext *stream_ctx;

static int open_input_file(EditorState *es) {
    int ret;
    unsigned int i;
    const char *filename = es->videoPath;

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
    stream_ctx = av_mallocz_array(ifmt_ctx->nb_streams, sizeof(*stream_ctx));
    if (!stream_ctx) {
        loge("Cannot av_mallocz_array stream_ctx\n");
        return AVERROR(ENOMEM);
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

        codec = avcodec_find_decoder(dec_ctx->codec_id);

        if (dec_ctx->codec_type == AVMEDIA_TYPE_VIDEO && es->mediaCodecDec) {
            codec = avcodec_find_decoder_by_name(ffeditor_hd_video_codec_name);
            if (!codec) {
                loge("硬解码器未找到，自动切换至软解\n");
                codec = avcodec_find_decoder(dec_ctx->codec_id);
            } else {
                logd("选择硬解码器\n");
            }
        }

        if (!codec) {
            loge("Necessary decoder not found\n");
            return AVERROR_DECODER_NOT_FOUND;
        } else {
            logd("find decoder : %s \n", codec->name);
        }
        dec_ctx->codec_id = codec->id;

        /* Reencode video & audio and remux subtitles etc. */
        if (dec_ctx->codec_type == AVMEDIA_TYPE_VIDEO ||
            dec_ctx->codec_type == AVMEDIA_TYPE_AUDIO) {
            if (dec_ctx->codec_type == AVMEDIA_TYPE_VIDEO) {
                dec_ctx->framerate = av_guess_frame_rate(ifmt_ctx, stream, NULL);
                es->rotation = get_rotation(stream);
            }
            /* 打开解码器 */
            ret = avcodec_open2(dec_ctx, codec, NULL);
            if (ret < 0) {
                loge("Failed to open decoder for stream #%u\n", i);
                return ret;
            }
        }
        stream_ctx[i].dec_ctx = dec_ctx;
    }
    av_dump_format(ifmt_ctx, 0, filename, 0);
    return 0;
}


static int open_output_file(EditorState *es) {
    AVStream *out_stream;
    AVStream *in_stream;
    AVCodecContext *dec_ctx, *enc_ctx;
    AVCodec *encoder;
    AVDictionary *encode_opts = NULL;
    int ret;
    unsigned int i;
    const char *filename;
    filename = es->outputPath;
    ofmt_ctx = NULL;

    avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, filename);
    if (!ofmt_ctx) {
        loge("Could not create output context\n");
        return AVERROR_UNKNOWN;
    }
    logd("video src : start_time=%lld  ; output : duration=%lld ",
         ifmt_ctx->start_time, ifmt_ctx->duration);

    //分别初始化各个流
    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        out_stream = avformat_new_stream(ofmt_ctx, NULL);
        if (!out_stream) {
            loge("Failed allocating output stream\n");
            return AVERROR_UNKNOWN;
        }
        in_stream = ifmt_ctx->streams[i];
        dec_ctx = stream_ctx[i].dec_ctx;

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

                //设置宽高，若未手动设置则默认与原视频相同
//                if (!es->outputWidth || !es->outputHeight) {
//                    es->outputWidth = dec_ctx->width;
//                    es->outputHeight = dec_ctx->height;
//                } else {
//                    int temp_outputWidth = es->outputWidth;
//                    es->outputWidth = es->rotation ? es->outputHeight : es->outputWidth;
//                    es->outputHeight = es->rotation ? temp_outputWidth : es->outputHeight;
//                }

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
                    logd("未手动设置输出码率，默认设置为%d ", ffeditor_default_output_bitrate);
                    es->outputBitrate = ffeditor_default_output_bitrate;
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
            if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
                enc_ctx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;

            out_stream->time_base = enc_ctx->time_base;
            stream_ctx[i].enc_ctx = enc_ctx;
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
    av_dump_format(ofmt_ctx, 0, filename, 1);

    if (!(ofmt_ctx->oformat->flags & AVFMT_NOFILE)) {
        ret = avio_open(&ofmt_ctx->pb, filename, AVIO_FLAG_WRITE);
        if (ret < 0) {
            loge("Could not open output file '%s'", filename);
            return ret;
        }
    }

    /* init muxer, write output file header */
    ret = avformat_write_header(ofmt_ctx, NULL);
    if (ret < 0) {
        loge("Error occurred when avformat_write_header\n");
        return ret;
    }

    return 0;
}

#if CONFIG_FILTER

static int init_filter(FilteringContext *fctx, AVCodecContext *dec_ctx,
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

static int init_filters(void) {
    const char *filter_spec;
    unsigned int i;
    int ret;
    filter_ctx = av_malloc_array(ifmt_ctx->nb_streams, sizeof(*filter_ctx));
    if (!filter_ctx)
        return AVERROR(ENOMEM);

    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        filter_ctx[i].buffersrc_ctx = NULL;
        filter_ctx[i].buffersink_ctx = NULL;
        filter_ctx[i].filter_graph = NULL;
        if (!(ifmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO
              || ifmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO))
            continue;


        if (ifmt_ctx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO)
            filter_spec = ffeditor_video_filter_spec;
        else
            filter_spec = ffeditor_audio_filter_spec;
        ret = init_filter(&filter_ctx[i], stream_ctx[i].dec_ctx,
                          stream_ctx[i].enc_ctx, filter_spec);
        if (ret)
            return ret;
    }
    return 0;
}

#endif

static int encode_write_frame(EditorState *es, AVFrame *filt_frame, unsigned int stream_index,
                              int *got_frame) {
    int ret;
    int got_frame_local;
    AVPacket enc_pkt;
    int (*enc_func)(AVCodecContext *, AVPacket *, const AVFrame *, int *) =
    (ifmt_ctx->streams[stream_index]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) ?
    avcodec_encode_video2 : avcodec_encode_audio2;

    if (!got_frame)
        got_frame = &got_frame_local;

    logd("Encoding frame\n");
    /* encode filtered frame */
    enc_pkt.data = NULL;
    enc_pkt.size = 0;
    av_init_packet(&enc_pkt);

    //todo gl渲染
//    int *da = uploadTexture(filt_frame);
//    onDrawFrame(da);
//    filt_frame->data[0] = readDataFromGPU(720, 1280);


    if (es->mediaCodecEnc) {
        ret = mediacodec_encode_frame(es, &enc_pkt, filt_frame);
    } else {
        ret = enc_func(stream_ctx[stream_index].enc_ctx, &enc_pkt, filt_frame, got_frame);
    }

    av_frame_free(&filt_frame);
    if (ret < 0) {
        loge("mediacodec_encode_frame ret = %d\n", ret);
        return 0;
    }
    if (!(*got_frame))
        return 0;

    /* prepare packet for muxing */
    enc_pkt.stream_index = stream_index;
    av_packet_rescale_ts(&enc_pkt,
                         stream_ctx[stream_index].enc_ctx->time_base,
                         ofmt_ctx->streams[stream_index]->time_base);

    logd("Muxing frame\n");
    /* mux encoded frame */
    //硬解的情况下有些帧  mux 会返回-22，无效参数（对于3S视频有7帧异常）
    ret = av_interleaved_write_frame(ofmt_ctx, &enc_pkt);
    if (ret < 0) {
        loge("Failed to muxing frame ret=%d\n", ret);
    }
    return 0;
}

#if CONFIG_FILTER

static int filter_encode_write_frame(EditorState *es, AVFrame *frame, unsigned int stream_index) {
    int ret;
    AVFrame *filt_frame;

    logd("Pushing decoded frame to filters\n");

    /* push the decoded frame into the filtergraph */
    ret = av_buffersrc_add_frame_flags(filter_ctx[stream_index].buffersrc_ctx,
                                       frame, 0);
    if (ret < 0) {
        loge("Error while feeding the filtergraph\n");
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
        ret = encode_write_frame(es, filt_frame, stream_index, NULL);
        if (ret < 0)
            break;
    }

    return ret;
}

#endif

static int flush_encoder(EditorState *es, unsigned int stream_index) {
    int ret;
    int got_frame;

    if (!(stream_ctx[stream_index].enc_ctx->codec->capabilities &
          AV_CODEC_CAP_DELAY))
        return 0;

    while (1) {
        logd("Flushing stream #%u encoder\n", stream_index);

        ret = encode_write_frame(es, NULL, stream_index, &got_frame);
        if (ret < 0)
            break;
        if (!got_frame)
            return 0;
    }
    return ret;
}

int ffeditor_save_thread(void *arg) {

    EditorState *es = arg;
    time_t c_start, c_end;
    c_start = time(NULL);    //!< 单位为s

    int ret;
    AVPacket packet = {.data = NULL, .size = 0};
    AVFrame *frame = NULL;
    enum AVMediaType type;
    unsigned int stream_index;
    unsigned int i;
    int got_frame;
    int (*dec_func)(AVCodecContext *, AVFrame *, int *, const AVPacket *);


    if ((ret = open_input_file(es)) < 0)
        goto end;
    if ((ret = open_output_file(es)) < 0)
        goto end;
#if CONFIG_FILTER
    if ((ret = init_filters()) < 0)
        goto end;
#endif
    /* read all packets */
    while (1) {
        if ((ret = av_read_frame(ifmt_ctx, &packet)) < 0)
            break;
        stream_index = packet.stream_index;
        type = ifmt_ctx->streams[packet.stream_index]->codecpar->codec_type;
        logd("Demuxer gave frame of stream_index %u\n", stream_index);

#if CONFIG_FILTER
        if (filter_ctx[stream_index].filter_graph) {
#else
#endif
            frame = av_frame_alloc();
            if (!frame) {
                ret = AVERROR(ENOMEM);
                break;
            }
            av_packet_rescale_ts(&packet,
                                 ifmt_ctx->streams[stream_index]->time_base,
                                 stream_ctx[stream_index].dec_ctx->time_base);
            dec_func = (type == AVMEDIA_TYPE_VIDEO) ? avcodec_decode_video2 :
                       avcodec_decode_audio4;
            logd("decoding frame\n");
            ret = dec_func(stream_ctx[stream_index].dec_ctx, frame, &got_frame, &packet);
            if (ret < 0) {
                av_frame_free(&frame);
                loge("Decoding failed\n");
                break;
            }
            if (got_frame) {
                logd("frame width=%d,height=%d\n", frame->width, frame->height);
                frame->pts = av_frame_get_best_effort_timestamp(frame);
                ret = filter_encode_write_frame(es, frame, stream_index);
                av_frame_free(&frame);
                if (ret < 0)
                    goto end;
            } else {
                av_frame_free(&frame);
            }
        } else {
            /* remux this frame without reencoding */
            av_log(NULL, AV_LOG_DEBUG, "remux this frame without reencoding\n");
            av_packet_rescale_ts(&packet,
                                 ifmt_ctx->streams[stream_index]->time_base,
                                 ofmt_ctx->streams[stream_index]->time_base);

            ret = av_interleaved_write_frame(ofmt_ctx, &packet);
            if (ret < 0)
                goto end;
        }
        av_packet_unref(&packet);
    }

    /* flush filters and encoders */
    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        /* flush filter */
#if CONFIG_FILTER
        if (!filter_ctx[i].filter_graph)
            continue;
#endif

#if CONFIG_FILTER
        ret = filter_encode_write_frame(es, frame, i);
#else
        //        ret = encode_write_frame(es, frame, i, NULL);
#endif
        if (ret < 0) {
            loge("Flushing filter failed\n");
            goto end;
        }

        /* flush encoder */
        ret = flush_encoder(es, i);
        if (ret < 0) {
            loge("Flushing encoder failed\n");
            goto end;
        }
    }

    av_write_trailer(ofmt_ctx);
    end:
    av_packet_unref(&packet);
    av_frame_free(&frame);
    for (i = 0; i < ifmt_ctx->nb_streams; i++) {
        avcodec_free_context(&stream_ctx[i].dec_ctx);
        if (ofmt_ctx && ofmt_ctx->nb_streams > i && ofmt_ctx->streams[i] && stream_ctx[i].enc_ctx)
            avcodec_free_context(&stream_ctx[i].enc_ctx);
#if CONFIG_FILTER
        if (filter_ctx && filter_ctx[i].filter_graph)
            avfilter_graph_free(&filter_ctx[i].filter_graph);
#endif
    }
#if CONFIG_FILTER
    av_free(filter_ctx);
#endif
    av_free(stream_ctx);
    avformat_close_input(&ifmt_ctx);
    if (ofmt_ctx && !(ofmt_ctx->oformat->flags & AVFMT_NOFILE))
        avio_closep(&ofmt_ctx->pb);
    avformat_free_context(ofmt_ctx);

    if (ret < 0) {
        loge("Error occurred: %s\n", av_err2str(ret));
    } else {
        logd("save done , outputPath: %s\n", es->outputPath);
    }


    c_end = time(NULL);
    logd("The save used %lf s by time()\n", difftime(c_end, c_start));
    return ret ? 1 : 0;

}


int ffeditor_save(EditorState *es) {

    //todo 硬保
    //todo 创建独立的解码线程和编码线程，帧队列
    //todo 进度回调
    //todo 插入 filter

    es->mediaCodecDec = true;
    es->mediaCodecEnc = false;

    if (es->mediaCodecEnc) {
        mediacodec_encode_init(es);
    }

    es->save_tid = SDL_CreateThreadEx(&es->_save_tid, ffeditor_save_thread, es,
                                      "ffeditor_save_thread");

    if (!es->save_tid) {
        av_log(NULL, AV_LOG_ERROR, "SDL_CreateThread(): %s\n", SDL_GetError());
        return AVERROR(ENOMEM);
    }
    return 0;
}



