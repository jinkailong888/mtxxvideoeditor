//
// Created by wyh3 on 2018/3/26.
//


#include "ff_ffmux_hard.h"
#include <ijkyuv/include/libyuv.h>

static jobject mHardMuxJni;
static jmethodID onVideoEncodeMethod = NULL;
static jmethodID onAudioEncodeMethod;
static jmethodID onVideoDoneMethod;
static jmethodID onAudioDoneMethod;
static JavaVM *g_jvm;
static JNIEnv *mEnv;


void ff_ffmux_hard_init(JavaVM *vm, JNIEnv *env) {
    g_jvm = vm;
    mEnv = env;
}

void ff_ffmux_set_HardMuxJni(JNIEnv *env, jobject instance, jobject hardMuxJni) {

    logd("ff_ffmux_set_HardMuxJni");

    if (mHardMuxJni) {
        (*env)->DeleteGlobalRef(env, mHardMuxJni);
    }
    if (hardMuxJni != NULL) {
        mHardMuxJni = (*env)->NewGlobalRef(env, hardMuxJni);
        logd("ff_ffmux_set_HardMuxJni NewGlobalRef hardMuxJni");
    }


}


void ff_ffmux_hard_release() {

}


void ff_ffmux_hard_onVideoEncode(unsigned char *rgbaData, double pts, int rgbSize, int width,
                                 int height) {
    JNIEnv *env;
    logd("onVideoEncode rgbSize=%d", rgbSize);

    int status = (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);

    if (status != JNI_OK) {
        loge("onVideoEncode JNI NOT OK!");
        return;
    }

    int ret;

    AVFrame *pYuvFrame = av_frame_alloc();
    int yuvSize = av_image_get_buffer_size(AV_PIX_FMT_YUV420P, width, height, 1);
    uint8_t *yuvBuffer = (uint8_t *) av_malloc(yuvSize * sizeof(uint8_t));
    if (!yuvBuffer) {
        loge("%s av_image_get_buffer_size failed\n", __func__);
        return;
    }
    ret = av_image_fill_arrays(pYuvFrame->data,
                               pYuvFrame->linesize,
                               yuvBuffer,
                               AV_PIX_FMT_YUV420P,
                               width,
                               height,
                               1);
    if (ret < 0) {
        loge("%s pYuvFrame av_image_fill_arrays failed\n", __func__);
        return;
    }

    RGBAToI420(rgbaData, width * 4,
               pYuvFrame->data[0], pYuvFrame->linesize[0],
               pYuvFrame->data[1], pYuvFrame->linesize[1],
               pYuvFrame->data[2], pYuvFrame->linesize[2],
               width, height);

    if (onVideoEncodeMethod == NULL) {
        jclass hardMuxJniClass = (*env)->GetObjectClass(env, mHardMuxJni);
        onVideoEncodeMethod = (*env)->GetMethodID(env, hardMuxJniClass, "onVideoFrame", "([BD)V");
    }

    if (onVideoEncodeMethod != NULL) {
        logd("onVideoEncodeMethod != null");
        jbyteArray array = (*env)->NewByteArray(env, yuvSize);
        (*env)->SetByteArrayRegion(env, array, 0, yuvSize, (const jbyte *) pYuvFrame->data);
        if (array == NULL) {
            loge("array = null");
            return;
        }
        (*env)->CallVoidMethod(env, mHardMuxJni, onVideoEncodeMethod, array, pts);
    }

    (*g_jvm)->DetachCurrentThread(g_jvm);
}

void ff_ffmux_hard_onVideoEncodeDone() {
    JNIEnv *env;
    logd("onVideoEncodeDone ");

    int status = (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);
    if (status != JNI_OK) {
        loge("onVideoEncodeDone JNI NOT OK!");
        return;
    }
    if (onVideoDoneMethod == NULL) {
        jclass hardMuxJniClass = (*env)->GetObjectClass(env, mHardMuxJni);
        onVideoDoneMethod = (*env)->GetMethodID(env, hardMuxJniClass, "onVideoDone", "()V");
    }

    if (onVideoDoneMethod != NULL) {
        logd("CallVoidMethod onVideoDoneMethod");
        (*env)->CallVoidMethod(env, mHardMuxJni, onVideoDoneMethod);
    }

    (*g_jvm)->DetachCurrentThread(g_jvm);
}


void ff_ffmux_hard_onAudioEncode(AVFrame *pFrame) {
    JNIEnv *env;
    jlong jpts = pFrame->pts;
    logd("ff_ffmux_hard_onAudioEncode size=%d,pts=%lld", pFrame->linesize[0], jpts);

    int status = (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);
    if (status != JNI_OK) {
        loge("onAudioEncode JNI NOT OK!");
        return;
    }
    if (onVideoEncodeMethod == NULL) {
        jclass hardMuxJniClass = (*env)->GetObjectClass(env, mHardMuxJni);
        onAudioEncodeMethod = (*env)->GetMethodID(env, hardMuxJniClass, "onAudioFrame", "([BJ)V");
    }
    if (onAudioEncodeMethod != NULL) {
        logd("onAudioEncodeMethod != null");

        int t_data_size = av_samples_get_buffer_size(
                NULL, pFrame->channels,
                pFrame->nb_samples,
                (enum AVSampleFormat) pFrame->format,
                0);



//        if(av_sample_fmt_is_planar((enum AVSampleFormat)pFrame->format))
//        {//如果是平面的
//            uint8_t *buf = (uint8_t *)malloc(t_data_size);
//            interleave(pFrame->data, buf,
//                       pFrame->channels, (enum AVSampleFormat)pFrame->format);
//
//        }
//        else
//        {
//
//            outPcmFilePtr->write((const char *)inFrame->data[0],t_data_size);
//            outPcmFilePtr->flush();
//        }
        int planar = av_sample_fmt_is_planar((enum AVSampleFormat) (pFrame->format));
        if (planar) {
            logd("音频数据为平面类型 ");
            //todo
        }


        jbyteArray array = (*env)->NewByteArray(env, t_data_size);
        (*env)->SetByteArrayRegion(env, array, 0, t_data_size,
                                   (const jbyte *) pFrame->extended_data);
        if (array == NULL) {
            loge("array = null");
            return;
        }
        (*env)->CallVoidMethod(env, mHardMuxJni, onAudioEncodeMethod, array, jpts);
    }

    (*g_jvm)->DetachCurrentThread(g_jvm);
}


void ff_ffmux_hard_onAudioEncodeDone() {
    JNIEnv *env;
    logd("ff_ffmux_hard_onAudioEncodeDone ");

    int status = (*g_jvm)->AttachCurrentThread(g_jvm, &env, NULL);
    if (status != JNI_OK) {
        loge("onVideoEncodeDone JNI NOT OK!");
        return;
    }
    if (onVideoDoneMethod == NULL) {
        jclass hardMuxJniClass = (*env)->GetObjectClass(env, mHardMuxJni);
        onAudioDoneMethod = (*env)->GetMethodID(env, hardMuxJniClass, "onAudioDone", "()V");
    }

    if (onAudioDoneMethod != NULL) {
        logd("CallVoidMethod onAudioDoneMethod");
        (*env)->CallVoidMethod(env, mHardMuxJni, onAudioDoneMethod);
    }

    (*g_jvm)->DetachCurrentThread(g_jvm);
}







