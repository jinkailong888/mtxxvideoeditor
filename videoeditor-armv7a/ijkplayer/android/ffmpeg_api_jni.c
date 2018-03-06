/*
 * ffmpeg_api_jni.c
 *
 * Copyright (c) 2014 Bilibili
 * Copyright (c) 2014 Zhang Rui <bbcallen@gmail.com>
 *
 * This file is part of ijkPlayer.
 *
 * ijkPlayer is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * ijkPlayer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ijkPlayer; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#include "ffmpeg_api_jni.h"
#include <libavutil/log.h>
#include <assert.h>
#include <string.h>
#include <jni.h>
#include "../ff_ffinc.h"
#include "ijksdl/ijksdl_log.h"
#include "ijksdl/android/ijksdl_android_jni.h"

#define JNI_CLASS_FFMPEG_API "tv/danmaku/ijk/media/player/ffmpeg/FFmpegApi"
#define LOGD(format, ...) av_log(NULL, AV_LOG_DEBUG, format, ##__VA_ARGS__);
#define LOGE(format, ...) av_log(NULL, AV_LOG_ERROR, format, ##__VA_ARGS__);

typedef struct ffmpeg_api_fields_t {
    jclass clazz;
} ffmpeg_api_fields_t;
static ffmpeg_api_fields_t g_clazz;

static jstring
FFmpegApi_av_base64_encode(JNIEnv *env, jclass clazz, jbyteArray in) {
    jstring ret_string = NULL;
    char *out_buffer = 0;
    int out_size = 0;
    jbyte *in_buffer = 0;
    jsize in_size = (*env)->GetArrayLength(env, in);
    if (in_size <= 0)
        goto fail;

    in_buffer = (*env)->GetByteArrayElements(env, in, NULL);
    if (!in_buffer)
        goto fail;

    out_size = AV_BASE64_SIZE(in_size);
    out_buffer = malloc(out_size + 1);
    if (!out_buffer)
        goto fail;
    out_buffer[out_size] = 0;

    if (!av_base64_encode(out_buffer, out_size, (const uint8_t *) in_buffer, in_size))
        goto fail;

    ret_string = (*env)->NewStringUTF(env, out_buffer);
    fail:
    if (in_buffer) {
        (*env)->ReleaseByteArrayElements(env, in, in_buffer, JNI_ABORT);
        in_buffer = NULL;
    }
    if (out_buffer) {
        free(out_buffer);
        out_buffer = NULL;
    }
    return ret_string;
}


AVFormatContext *ic;
int video_stream_idx;
int audio_stream_idx;

static jint
FFmpegApi_open_video(JNIEnv *env, jclass clazz, jstring url) {
    const char *videoUrl = NULL;
    videoUrl = (*env)->GetStringUTFChars(env, url, NULL);
    LOGE("FFmpegApi_open_video url : %s", videoUrl);
    ic = avformat_alloc_context();
    if (avformat_open_input(&ic, videoUrl, NULL, NULL) < 0) {
        LOGE("could not open source %s", videoUrl);
        return -1;
    }
    if (avformat_find_stream_info(ic, NULL) < 0) {
        LOGE("could not find stream information");
        return -1;
    }
    video_stream_idx = av_find_best_stream(ic, AVMEDIA_TYPE_VIDEO, -1, -1, NULL, 0);
//    audio_stream_idx = av_find_best_stream(ic, AVMEDIA_TYPE_AUDIO, -1, -1, NULL, 0);
    return 0;
}

static jint
FFmpegApi_get_video_width(JNIEnv *env, jclass clazz) {
    if (video_stream_idx >= 0) {
        AVStream *video_stream = ic->streams[video_stream_idx];
        return video_stream->codecpar->width;
    }else{
        return 0;
    }
}

static jint
FFmpegApi_get_video_height(JNIEnv *env, jclass clazz) {
    if (video_stream_idx >= 0) {
        AVStream *video_stream = ic->streams[video_stream_idx];
        return video_stream->codecpar->height;
    }else{
        return 0;
    }
}

static jlong
FFmpegApi_get_video_duration(JNIEnv *env, jclass clazz) {
    jlong duration = ic->duration;
    return duration;
}

static jstring
FFmpegApi_get_video_codec_name(JNIEnv *env, jclass clazz) {
    if (video_stream_idx >= 0) {
        AVStream *video_stream = ic->streams[video_stream_idx];
        const char *codec_name = avcodec_get_name(video_stream->codecpar->codec_id);
        return (*env)->NewStringUTF(env, codec_name);
    }else{
        return NULL;
    }
}


static void
FFmpegApi_close_video(JNIEnv *env, jclass clazz) {
    avformat_close_input(&ic);
}


static JNINativeMethod g_methods[] = {
        {"av_base64_encode",   "([B)Ljava/lang/String;", (void *) FFmpegApi_av_base64_encode},
        {"_open",              "(Ljava/lang/String;)I",  (void *) FFmpegApi_open_video},
        {"_getVideoWidth",     "()I",                    (void *) FFmpegApi_get_video_width},
        {"_getVideoHeight",    "()I",                    (void *) FFmpegApi_get_video_height},
        {"_getVideoDuration",  "()J",                    (void *) FFmpegApi_get_video_duration},
        {"_getVideoCodecName", "()Ljava/lang/String;",   (void *) FFmpegApi_get_video_codec_name},
        {"close",              "()V",                    (void *) FFmpegApi_close_video},
};

int FFmpegApi_global_init(JNIEnv *env) {
    int ret = 0;

    IJK_FIND_JAVA_CLASS(env, g_clazz.clazz, JNI_CLASS_FFMPEG_API);
    (*env)->RegisterNatives(env, g_clazz.clazz, g_methods, NELEM(g_methods));

    return ret;
}
