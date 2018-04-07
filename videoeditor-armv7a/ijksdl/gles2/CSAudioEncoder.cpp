////
////  CSAudioEncoder.cpp
////  Sample
////
////  Created by zj-db0519 on 2017/12/25.
////  Copyright © 2017年 meitu. All rights reserved.
////
//
//#include "CSAudioEncoder.h"
//
//namespace HBMedia {
//
//int CSAudioEncoder::S_MAX_BUFFER_CACHE = 8;
//
//void* CSAudioEncoder::ThreadFunc_Audio_Encoder(void *arg) {
//    if (!arg) {
//        LOGE("[Work task: <Encoder>] Thread param args is invalid !");
//        return nullptr;
//    }
//
//    int HBError = HB_OK;
//    ThreadParam_t *pThreadParams = (ThreadParam_t *)arg;
//    CSAudioEncoder* pEncoder = (CSAudioEncoder *)(pThreadParams->mThreadArgs);
//    AVPacket *pNewPacket = av_packet_alloc();
//    AVFrame *pInFrame = nullptr;
//    AVFrame *pOutFrame = nullptr;
//
//    while (S_NOT_EQ(pEncoder->mState, S_READ_DATA_END) \
//           || (pEncoder->mSrcFrameQueue->queueLength() > 0))
//    {
//        pInFrame = nullptr;
//        pOutFrame = nullptr;
//
//        if (S_EQ(pEncoder->mState, S_ENCODE_ABORT) \
//            || S_EQ(pEncoder->mState, S_ABORT))
//            break;
//
//        pEncoder->mSrcFrameQueueIPC->condV();
//        if (!(pInFrame = pEncoder->mSrcFrameQueue->get())) {
//            LOGE("[Work task: <Encoder:%p>] Get queue[%p] length:%d frame failed !", pEncoder, \
//                 pEncoder->mSrcFrameQueue, pEncoder->mSrcFrameQueue->queueLength());
//            continue;
//        }
//        pEncoder->mEmptyFrameQueueIPC->condP();
//
//        pInFrame->pts = (int64_t)av_rescale_q(pInFrame->pts, AV_TIME_BASE_Q, (AVRational){1, pEncoder->mSrcAudioParams.sample_rate});
//        if (pEncoder->mIsNeedTransfer) {
//            bool bIsResampled = true;
//            if (pEncoder->_DoResample(pInFrame, &pOutFrame) != HB_OK)
//                bIsResampled = false;
//
//            disposeImageFrame(&pInFrame);
//            if (!bIsResampled) {
//                LOGE("[Work task: <Encoder:%p>] Do audio resample failed !", pEncoder);
//                continue;
//            }
//        }
//        else {
//            pOutFrame = pInFrame;
//            pInFrame = nullptr;
//        }
//
//        /** 缓冲音频数据 */
//        if (pEncoder->mAudioDataCacheObj.WriteDataToCache(pOutFrame) != 1)
//            LOGE("[Work task: <Encoder>] Buffer audio frame data failed !");
//
//        disposeImageFrame(&pOutFrame);
//        if (pEncoder->mAudioDataCacheObj.ReadDataFromCache(&pOutFrame) != 1) {
//            if (pOutFrame)
//                disposeImageFrame(&pOutFrame);
//            continue;
//        }
//
//        HBError = avcodec_send_frame(pEncoder->mPOutAudioCodecCtx, pOutFrame);
//        disposeImageFrame(&pOutFrame);
//
//        if (HBError != 0) {
//            if (HBError != AVERROR(EAGAIN)) {
//                LOGE("[Work task: <Encoder>] Send frame failed, Err:%s", av_err2str(HBError));
//                pEncoder->mState |= S_ENCODE_ABORT;
//            }
//            continue;
//        }
//
//        while (true) {
//            HBError = avcodec_receive_packet(pEncoder->mPOutAudioCodecCtx, pNewPacket);
//            if (HBError == 0) {
//                pNewPacket->stream_index = pEncoder->mAudioStreamIndex;
//                HBError = pEncoder->_DoExport(pNewPacket);
//                if (HBError != HB_OK) {
//                    av_packet_unref(pNewPacket);
//                }
//            }
//            else {
//                if (HBError == AVERROR(EAGAIN))
//                    break;
//                else if (HBError != AVERROR_EOF) {
//                    LOGE("[Work task: <Encoder>] Receive packet failed, Err:%s", av_err2str(HBError));
//                    pEncoder->mState |= S_ENCODE_ABORT;
//                }
//            }
//        }
//    }
//
//    pEncoder->_flush();
//
//VIDEO_ENCODER_THREAD_END_LABEL:
//    if ((HBError = av_write_trailer(pEncoder->mPOutMediaFormatCtx)) != 0)
//        LOGE("[Work task: <Encoder>] write trail failed, %s !", av_err2str(HBError));
//
//    avcodec_close(pEncoder->mPOutAudioCodecCtx);
//    avio_close(pEncoder->mPOutMediaFormatCtx->pb);
//    avformat_free_context(pEncoder->mPOutMediaFormatCtx);
//    pEncoder->mPOutMediaFormatCtx = nullptr;
//
//    pEncoder->mState |= S_FINISHED;
//    av_packet_free(&pNewPacket);
//
//    if (pInFrame) {
//        if (pInFrame->opaque)
//            av_freep(pInFrame->opaque);
//        av_frame_free(&pInFrame);
//    }
//    if (pOutFrame) {
//        if (pOutFrame->opaque)
//            av_freep(pOutFrame->opaque);
//        av_frame_free(&pOutFrame);
//    }
//    return nullptr;
//}
//
//CSAudioEncoder::CSAudioEncoder() {
//    memset(&mState, 0x00, sizeof(mState));
//    mRoleType = T_AUDIO_ENCODER;
//    mAudioStreamIndex = INVALID_STREAM_INDEX;
//
//    mPAudioResampleCtx = nullptr;
//    mSrcFrameQueue = new FiFoQueue<AVFrame *>(S_MAX_BUFFER_CACHE);
//    mSrcFrameQueueIPC = new ThreadIPCContext(0);
//    mEmptyFrameQueueIPC = new ThreadIPCContext(S_MAX_BUFFER_CACHE);
//    mEncodeThreadCtx.setFunction(nullptr, nullptr);
//}
//
//CSAudioEncoder::~CSAudioEncoder() {
//}
//
//int CSAudioEncoder::prepare() {
//    if (baseInitial() != HB_OK) {
//        LOGE("Audio base initial failed !");
//        goto AUDIO_ENCODER_PREPARE_END_LABEL;
//    }
//
//    if (_mediaParamInitial() != HB_OK) {
//        LOGE("Check Audio encoder param failed !");
//        goto AUDIO_ENCODER_PREPARE_END_LABEL;
//    }
//
//    if (_InputInitial() != HB_OK) {
//        LOGE("Audio decoder open failed !");
//        goto AUDIO_ENCODER_PREPARE_END_LABEL;
//    }
//
//    if (_EncoderInitial() != HB_OK) {
//        LOGE("Audio encoder initial failed !");
//        goto AUDIO_ENCODER_PREPARE_END_LABEL;
//    }
//
//    if (_ResampleInitial() != HB_OK) {
//        LOGE("Audio swscale initail failed !");
//        goto AUDIO_ENCODER_PREPARE_END_LABEL;
//    }
//
//    /** 创建音频缓冲区 */
//    mAudioDataCacheObj.setAudioParams(&mTargetAudioParams);
//    if(HB_OK != mAudioDataCacheObj.CacheInitial()) {
//        LOGE("Audio encoder initial audio inner data buffer failed !");
//        return HB_ERROR;
//    }
//
//    mState |= S_PREPARED;
//    return HB_OK;
//
//AUDIO_ENCODER_PREPARE_END_LABEL:
//    release();
//    return HB_ERROR;
//}
//
//int CSAudioEncoder::start() {
//    if (!(mState & S_PREPARED)) {
//        LOGE("Media audio encoder is not prepared !");
//        return HB_ERROR;
//    }
//    if (HB_OK != mEncodeThreadCtx.setFunction(ThreadFunc_Audio_Encoder, this)) {
//        LOGE("Initial encode thread context failed !");
//        return HB_ERROR;
//    }
//
//    if (mEncodeThreadCtx.start() != HB_OK) {
//        LOGE("Start encode thread context failed !");
//        return HB_ERROR;
//    }
//
//    return HB_OK;
//}
//
//int CSAudioEncoder::stop() {
//    if (!(mState & S_DECODE_END)) {
//        mAbort = true;
//    }
//    mEncodeThreadCtx.join();
//
//    if (CSMediaBase::stop() != HB_OK) {
//        LOGE("Media base stop failed !");
//        return HB_ERROR;
//    }
//
//    AVFrame *pFrame = nullptr;
//    while (mSrcFrameQueue->queueLength()) {
//        pFrame = mSrcFrameQueue->get();
//        if (pFrame) {
//            if (pFrame->opaque)
//                av_freep(pFrame);
//            av_frame_free(&pFrame);
//        }
//    }
//
//    if (release() != HB_OK) {
//        LOGE("decoder release failed !");
//        return HB_ERROR;
//    }
//
//    mAudioDataCacheObj.release();
//    return HB_OK;
//}
//
//int CSAudioEncoder::release() {
//    if (mPOutAudioCodecCtx)
//        avcodec_free_context(&mPOutAudioCodecCtx);
//    mPOutAudioCodecCtx = nullptr;
//    if (mPAudioResampleCtx) {
//        swr_free(&mPAudioResampleCtx);
//        mPAudioResampleCtx = nullptr;
//    }
//    CSMediaBase::release();
//    return HB_OK;
//}
//
//int CSAudioEncoder::sendFrame(AVFrame **pSrcFrame) {
//    if (S_NOT_EQ(mState, S_PREPARED) \
//        || S_EQ(mState, S_READ_DATA_END) \
//        || (mInMediaType != MD_TYPE_RAW_BY_MEMORY)) {
//        LOGE("Audio Encoder >>> Audio encoder status abnormal !");
//        return HB_ERROR;
//    }
//
//    if (pSrcFrame == nullptr) {/** 进入刷帧模式 */
//        mState |= S_READ_DATA_END;
//        LOGI("Audio Encoder >>> Send frame end !");
//        return HB_OK;
//    }
//
//    if (!(*pSrcFrame)) {
//        LOGE("Audio Encoder >>> Invalid frame, send failed !");
//        return HB_ERROR;
//    }
//
//RETRY_SEND_FRAME:
//    if (mSrcFrameQueue \
//        && S_NOT_EQ(mState, S_ENCODE_ABORT) \
//        && S_NOT_EQ(mState, S_ENCODE_FLUSHING))
//    {
//        mEmptyFrameQueueIPC->condV();
//        if (mSrcFrameQueue->queueLeft() > 0) {
//            LOGD("Audio Encoder >>> [Extern] Send audio frame, pts<%lld, %lf> !", (*pSrcFrame)->pts, \
//                                                    ((*pSrcFrame)->pts * av_q2d(AV_TIME_BASE_Q)));
//            if (mSrcFrameQueue->push(*pSrcFrame) > 0) {
//                mSrcFrameQueueIPC->condP();
//                return HB_OK;
//            }
//        }
//        else {
//            goto RETRY_SEND_FRAME;
//        }
//    }
//
//    return HB_ERROR;
//}
//
//int CSAudioEncoder::syncWait() {
//    while (S_NOT_EQ(mState, S_FINISHED)) {
//        usleep(100);
//    }
//    return HB_OK;
//}
//
//int CSAudioEncoder::_mediaParamInitial() {
//    mIsNeedTransfer = false;
//    switch (mInMediaType) {
//        case MD_TYPE_UNKNOWN:
//        {
//            if (mSrcMediaFile)
//                mInMediaType = MD_TYPE_COMPRESS;
//            else {
//                LOGE("[%s] >>> [Type:%s] Audio encoder input file is invalid !", __func__, getMediaDataTypeDescript(mInMediaType));
//                return HB_ERROR;
//            }
//        }
//            break;
//
//        case MD_TYPE_RAW_BY_FILE:
//            LOGE("Remain to support");
//            return HB_ERROR;
//
//        case MD_TYPE_RAW_BY_MEMORY:
//        {
//            if (!mSrcFrameQueue || !mSrcFrameQueueIPC || !mEmptyFrameQueueIPC) {
//                LOGE("[%s] >>> [Type:%s]Audio encoder output prepare failed !", __func__, getMediaDataTypeDescript(mOutMediaType));
//                return HB_ERROR;
//            }
//
//            if (mSrcAudioParams.pri_sample_fmt == CS_SAMPLE_FMT_NONE \
//                || mSrcAudioParams.sample_rate == 0 \
//                || mSrcAudioParams.channels == 0) {
//                LOGE("[%s] >>> [Type:%s]Audio encoder output params invalid !", __func__, getMediaDataTypeDescript(mOutMediaType));
//                return HB_ERROR;
//            }
//
//        }
//            break;
//
//        case MD_TYPE_COMPRESS:
//            if (!mSrcMediaFile) {
//                LOGE("[%s] >>> [Type:%s]Audio encoder input file is invalid !", __func__, getMediaDataTypeDescript(mInMediaType));
//                return HB_ERROR;
//            }
//            break;
//
//        default:
//            LOGE("Unknown media type !");
//            return HB_ERROR;
//    }
//
//    switch (mOutMediaType) {
//        case MD_TYPE_UNKNOWN:
//        {
//            if (mTrgMediaFile)
//                mOutMediaType = MD_TYPE_COMPRESS;
//            else {
//                LOGE("[%s] >>> [Type:%s] Audio encoder output file is invalid !", __func__, getMediaDataTypeDescript(mInMediaType));
//                return HB_ERROR;
//            }
//        }
//            break;
//
//        case MD_TYPE_RAW_BY_FILE:
//        case MD_TYPE_RAW_BY_MEMORY:
//            LOGE("Current not support the output type !");
//            return HB_ERROR;
//
//        case MD_TYPE_COMPRESS:
//            break;
//
//        default:
//            LOGE("Unknown media type !");
//            return HB_ERROR;
//    }
//    return HB_OK;
//}
//
//int CSAudioEncoder::_EncoderInitial() {
//    if (mOutMediaType == MD_TYPE_COMPRESS) {
//        int HBError = HB_OK;
//        AVDictionary *opts = NULL;
//        AVStream* pAudioStream = nullptr;
//        mPOutAudioCodecCtx = nullptr;
//        memset(&mState, 0x00, sizeof(mState));
//        if (CSMediaBase::_OutMediaInitial() != HB_OK) {
//            LOGE("Media base in media initial failed !");
//            goto AUDIO_ENCODER_INITIAL_END_LABEL;
//        }
//
//        pAudioStream = avformat_new_stream(mPOutMediaFormatCtx, NULL);
//        if (!pAudioStream) {
//            LOGE("[%s] >>> Audio encoder initial failed, new stream failed !", __func__);
//            goto AUDIO_ENCODER_INITIAL_END_LABEL;
//        }
//        mAudioStreamIndex = pAudioStream->index;
//        pAudioStream->time_base.num = 1;
//        pAudioStream->time_base.den = mTargetAudioParams.sample_rate;
//
//        mPOutAudioCodec = avcodec_find_encoder(mPOutMediaFormatCtx->oformat->audio_codec);
//        if (!mPOutAudioCodec) {
//            LOGE("[%s] >>> Audio encoder initial failed, find valid encoder failed !", __func__);
//            goto AUDIO_ENCODER_INITIAL_END_LABEL;
//        }
//
//        mPOutAudioCodecCtx = avcodec_alloc_context3(mPOutAudioCodec);
//        mPOutAudioCodecCtx->codec_id = mPOutAudioCodec->id;
//        mPOutAudioCodecCtx->codec_type = AVMEDIA_TYPE_AUDIO;
//        mPOutAudioCodecCtx->sample_fmt = getAudioInnerFormat(mTargetAudioParams.pri_sample_fmt);
//        mPOutAudioCodecCtx->sample_rate = mTargetAudioParams.sample_rate;
//        mPOutAudioCodecCtx->channels = mTargetAudioParams.channels;
//        mPOutAudioCodecCtx->channel_layout = av_get_default_channel_layout(mPOutAudioCodecCtx->channels);
//        mPOutAudioCodecCtx->bit_rate = mTargetAudioParams.mbitRate;
//
//        avcodec_parameters_from_context(pAudioStream->codecpar, mPOutAudioCodecCtx);
//
//        av_dump_format(mPOutMediaFormatCtx, 0, mTrgMediaFile, 1);
//
//        if ((HBError = avio_open(&(mPOutMediaFormatCtx->pb), mTrgMediaFile, AVIO_FLAG_READ_WRITE)) < 0) {
//            LOGE("Video encoder Could't open output file, %s !", makeErrorStr(HBError));
//            goto AUDIO_ENCODER_INITIAL_END_LABEL;
//        }
//
//        av_dict_set(&opts, "threads", "auto", 0);
//        if ((HBError = avcodec_open2(mPOutAudioCodecCtx, mPOutAudioCodec, &opts)) < 0) {
//            LOGE("Audio encoder open failed, %s!", makeErrorStr(HBError));
//            goto AUDIO_ENCODER_INITIAL_END_LABEL;
//        }
//
//        if ((HBError = avformat_write_header(mPOutMediaFormatCtx, NULL)) < 0) {
//            LOGE("Audio encoder write format header failed, %s!", makeErrorStr(HBError));
//            goto AUDIO_ENCODER_INITIAL_END_LABEL;
//        }
//    }
//    return HB_OK;
//AUDIO_ENCODER_INITIAL_END_LABEL:
//    CSMediaBase::release();
//    if (mPOutAudioCodecCtx)
//        avcodec_free_context(&mPOutAudioCodecCtx);
//    return HB_ERROR;
//}
//
//int CSAudioEncoder::_InputInitial() {
//    switch (mInMediaType) {
//        case MD_TYPE_RAW_BY_MEMORY:
//            if (!mSrcFrameQueue || !mSrcFrameQueueIPC || !mEmptyFrameQueueIPC) {
//                LOGE("[%s] >>> [Type:%s]Audio encoder input prepare failed !", __FUNCTION__, \
//                     getMediaDataTypeDescript(mInMediaType));
//                return HB_ERROR;
//            }
//            break;
//
//        case MD_TYPE_COMPRESS:
//            LOGE("[%s] >>> [Type:%s]Audio encoder not susport this media data type !", __FUNCTION__, \
//                 getMediaDataTypeDescript(mInMediaType));
//            break;
//
//        default:
//            LOGE("input media initial failed, cur media type:%s", getMediaDataTypeDescript(mInMediaType));
//            return HB_ERROR;
//    }
//
//    if (mTargetAudioParams.pri_sample_fmt == CS_SAMPLE_FMT_NONE)
//        mTargetAudioParams.pri_sample_fmt = mSrcAudioParams.pri_sample_fmt;
//    if (mTargetAudioParams.channels == 0)
//        mTargetAudioParams.channels = mSrcAudioParams.channels;
//    mTargetAudioParams.channel_layout = av_get_default_channel_layout(mTargetAudioParams.channels);
//    if (mTargetAudioParams.sample_rate == 0)
//        mTargetAudioParams.sample_rate = mSrcAudioParams.sample_rate;
//    if (mTargetAudioParams.mbitRate == 0)
//        mTargetAudioParams.mbitRate = mSrcAudioParams.mbitRate;
//
//    return HB_OK;
//}
//
//int CSAudioEncoder::_ResampleInitial() {
//    mIsNeedTransfer = false;
//    if (mSrcAudioParams.channel_layout != mTargetAudioParams.channel_layout \
//        || mSrcAudioParams.pri_sample_fmt != mTargetAudioParams.pri_sample_fmt \
//        || mSrcAudioParams.sample_rate != mTargetAudioParams.sample_rate) {
//
//        mIsNeedTransfer = true;
//        mPAudioResampleCtx = swr_alloc_set_opts(nullptr, \
//                                                mTargetAudioParams.channel_layout, getAudioInnerFormat(mTargetAudioParams.pri_sample_fmt), mTargetAudioParams.sample_rate,\
//                                                mSrcAudioParams.channel_layout, getAudioInnerFormat(mSrcAudioParams.pri_sample_fmt), mSrcAudioParams.sample_rate, \
//                                                0, nullptr);
//        if (!mPAudioResampleCtx) {
//            LOGE("Set audio resample context info failed !");
//            return HB_ERROR;
//        }
//
//        swr_init(mPAudioResampleCtx);
//    }
//
//    return HB_OK;
//}
//
//int CSAudioEncoder::_DoResample(const AVFrame *pInFrame, AVFrame **pOutFrame) {
//    *pOutFrame = nullptr;
//    if (!pInFrame || !mPAudioResampleCtx) {
//        LOGE("[Work task: <Encoder>] Audio resample args invalid !");
//        return HB_ERROR;
//    }
//
//    int HBError = HB_OK;
//    uint8_t *pInAudioBuffer[CS_SWR_CH_MAX] = {NULL};
//    AVFrame* pTargetFrame = av_frame_alloc();
//    if (!pTargetFrame) {
//        LOGE("[Work task: <Encoder>] Audio resample malloc audio target frame failed !");
//        goto AUDIO_RESAMPLE_END_LABEL;
//    }
//
//    /**
//     *  av_rescale_rnd(int64_t a, int64_t b, int64_t c, enum AVRounding rnd);
//     *  将以 "时钟基c" 表示的 数值a 转换成以 "时钟基b" 来表示, "a * b / c" 的值并分五种方式来取整
//     */
//    pTargetFrame->nb_samples = \
//    (int)av_rescale_rnd(swr_get_delay(mPAudioResampleCtx, mSrcAudioParams.sample_rate) + pInFrame->nb_samples,\
//                        mTargetAudioParams.sample_rate, mSrcAudioParams.sample_rate, AV_ROUND_UP);
//
//    HBError = av_samples_alloc(pTargetFrame->data, &pTargetFrame->linesize[0],
//                        mTargetAudioParams.channels, pTargetFrame->nb_samples, getAudioInnerFormat(mTargetAudioParams.pri_sample_fmt), mTargetAudioParams.mAlign);
//    if (HBError < 0) {
//        LOGE("[Work task: <Encoder>] Audio resample malloc output samples buffer failed !");
//        goto AUDIO_RESAMPLE_END_LABEL;
//    }
//
//    pTargetFrame->opaque = pTargetFrame->data[0];
//    HBError = swr_convert(mPAudioResampleCtx, pTargetFrame->data, pTargetFrame->nb_samples, \
//                          (const uint8_t **)pInFrame->data, pInFrame->nb_samples);
//    if (HBError < 0) {
//        LOGE("[Work task: <Encoder>] Audio resample swr convert failed ! %s", av_err2str(HBError));
//        goto AUDIO_RESAMPLE_END_LABEL;
//    }
//
//    pTargetFrame->pts = (int64_t)av_rescale_q(pInFrame->pts, \
//                                              (AVRational){1, mSrcAudioParams.sample_rate},
//                                              (AVRational){1, mTargetAudioParams.sample_rate});
//    pTargetFrame->nb_samples = HBError;
//    *pOutFrame = pTargetFrame;
//    return HB_OK;
//
//AUDIO_RESAMPLE_END_LABEL:
//    if (pTargetFrame) {
//        if (pTargetFrame->opaque)
//            av_freep(pTargetFrame->opaque);
//        av_frame_free(&pTargetFrame);
//    }
//    return HB_ERROR;
//}
//
//int CSAudioEncoder::_DoExport(AVPacket *pPacket) {
//    int HbError = av_write_frame(mPOutMediaFormatCtx, pPacket);
//    if (HbError != 0) {
//        LOGE("[Work task: <Encoder>] Export new packet failed, %s !", av_err2str(HbError));
//        mState |= S_ABORT;
//        return HB_ERROR;
//    }
//    av_packet_unref(pPacket);
//    return HB_OK;
//}
//
//void CSAudioEncoder::_flushDataCache() {
//    AVPacket *pNewPacket = av_packet_alloc();
//    AVFrame *pNewFrame = nullptr;
//    int HBError = HB_OK;
//
//    while ((S_NOT_EQ(mState, S_ABORT)) && (mAudioDataCacheObj.getCacheSize() > 0))
//    {
//        HBError = mAudioDataCacheObj.FlushDataCache(&pNewFrame);
//        if (HBError != 1) {
//            if (pNewFrame)
//                disposeImageFrame(&pNewFrame);
//            if (HBError == -2)
//                break;
//            continue;
//        }
//
//        HBError = avcodec_send_frame(mPOutAudioCodecCtx, pNewFrame);
//        disposeImageFrame(&pNewFrame);
//
//        if (HBError != 0) {
//            if (HBError != AVERROR(EAGAIN)) {
//                LOGE("[Work task: <Encoder>] Send frame failed, Err:%s", av_err2str(HBError));
//                mState |= S_ENCODE_ABORT;
//            }
//            continue;
//        }
//
//        while (true) {
//            HBError = avcodec_receive_packet(mPOutAudioCodecCtx, pNewPacket);
//            if (HBError == 0) {
//                pNewPacket->stream_index = mAudioStreamIndex;
//                HBError = _DoExport(pNewPacket);
//                if (HBError != HB_OK) {
//                    av_packet_unref(pNewPacket);
//                }
//            }
//            else {
//                if (HBError == AVERROR(EAGAIN))
//                    break;
//                else if (HBError != AVERROR_EOF) {
//                    LOGE("[Work task: <Encoder>] Receive packet failed, Err:%s", av_err2str(HBError));
//                    mState |= S_ENCODE_ABORT;
//                }
//            }
//        }
//    }
//
//    LOGI("[Work task: <Encoder>] Source frame queue size:%d, audio fifo size:%d", \
//         mSrcFrameQueue->queueLength(), mAudioDataCacheObj.getCacheSize());
//    av_packet_free(&pNewPacket);
//}
//
//void CSAudioEncoder::_flush() {
//    _flushDataCache();
//
//    int HBError = HB_OK;
//    AVPacket *pNewPacket = av_packet_alloc();
//
//    mState |= S_ENCODE_FLUSHING;
//    avcodec_send_frame(mPOutAudioCodecCtx, NULL);
//    while (true)
//    {
//        if (S_EQ(mState, S_ABORT))
//            break;
//
//        HBError = avcodec_receive_packet(mPOutAudioCodecCtx, pNewPacket);
//        if (HBError == 0) {
//            pNewPacket->stream_index = mAudioStreamIndex;
//            HBError = _DoExport(pNewPacket);
//            if (HBError != HB_OK) {
//                av_packet_unref(pNewPacket);
//            }
//        }
//        else {
//            if (HBError == AVERROR_EOF) {
//                mState |= S_ENCODE_END;
//                break;
//            } else {
//                LOGE("[Work task: <Encoder>] Receive packet failed, Err:%s", av_err2str(HBError));
//                mState |= S_ABORT;
//            }
//        }
//    }
//
//    av_packet_free(&pNewPacket);
//    EchoStatus(mState);
//}
//
//}