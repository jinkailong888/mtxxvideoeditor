package com.meitu.library.videoeditor.media.save;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import com.meitu.library.videoeditor.media.codec.CodecOutputSurface;
import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by wyh3 on 2018/3/22.
 * 保存任务
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SaveTask implements Runnable {
    private final static String TAG = Tag.build("SaveTask");

    private final static String VIDEO_ENCODE_MIME = "video/avc";
    private final static String AUDIO_ENCODE_MIME = "video/avc";
    private static final int DECODE_TIMEOUT = 1000;
    private static final int ENCODE_TIMEOUT = 1000;
    //分离器
    private MediaExtractor mVideoMediaExtractor;
    private MediaExtractor mBgMusicMediaExtractor;
    //编解码器
    private MediaCodec mVideoDecoder;
    private MediaCodec mAudioDecoder;
    private MediaCodec mBgMusicDecoder;
    private MediaCodec mVideoEncoder;
    private MediaCodec mAudioEncoder;
    //分离轨道index
    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;
    private int mBgMusicTrackIndex = -1;
    //分离数据format
    private MediaFormat mVideoTrackFormat;
    private MediaFormat mAudioTrackFormat;
    //surface
    private Surface mDecodeSurface;
    private Surface mEncodeSurface;
    private CodecOutputSurface mCodecOutputSurface;
    //inputBuffer
    ByteBuffer[] mVideoDecodeInputBuffer;
    ByteBuffer[] mAudioDecodeInputBuffer;
    //视频封装器
    private MediaMuxer mMediaMuxer;
    //读包线程
    private Runnable mReadPacketRunnable;
    //视频渲染、编码、取出线程
    private Runnable mVideoRunnable;
    //音频处理线程
    private Runnable mAudioHandleRunnable;
    //背景音乐处理线程
    private Runnable mBgMusicHandleRunnable;
    //音频编码线程
    private Runnable mAudioRunnable;
    //保存进度监听
    private OnSaveListener mOnSaveListener;
    //视频输出信息
    private VideoSaveInfo mVideoSaveInfo;
    //视频渲染效果
    private SaveFilters mSaveFilters;

    private void prepare() throws IOException {
        mMediaMuxer = new MediaMuxer(mVideoSaveInfo.getVideoSavePath(),
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        MediaFormat videoEncodeFormat = MediaFormat.createVideoFormat(VIDEO_ENCODE_MIME,
                mVideoSaveInfo.getOutputWidth(), mVideoSaveInfo.getOutputHeight());
        videoEncodeFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoEncodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoSaveInfo.getOutputBitrate());
        videoEncodeFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mVideoSaveInfo.getFps());
        videoEncodeFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mVideoSaveInfo.getIFrameInterval());
        mVideoEncoder = MediaCodec.createEncoderByType(VIDEO_ENCODE_MIME);
        mVideoEncoder.configure(videoEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncodeSurface = mVideoEncoder.createInputSurface();
        mVideoEncoder.start();

        mVideoMediaExtractor = new MediaExtractor();
        mVideoMediaExtractor.setDataSource(mVideoSaveInfo.getSrcPath());
        for (int i = 0; i < mVideoMediaExtractor.getTrackCount(); i++) {
            MediaFormat format = mVideoMediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            Log.d(TAG, "getTrackFormat index=" + i + " (" + mime + "): " + format);
            if (mime.startsWith("video/")) {
                mVideoTrackIndex = i;
                mVideoTrackFormat = format;
                mVideoDecoder = MediaCodec.createDecoderByType(mime);
                mCodecOutputSurface = new CodecOutputSurface(
                        mVideoSaveInfo.getOutputWidth(), mVideoSaveInfo.getOutputHeight(),
                        mEncodeSurface, mSaveFilters.isFilter());
                mVideoDecoder.configure(format, mCodecOutputSurface.getSurface(), null, 0);
                mVideoDecoder.start();
            }
            if (mime.startsWith("audio/")) {
                mAudioTrackIndex = i;
                mAudioTrackFormat = format;
                mAudioDecoder = MediaCodec.createDecoderByType(mime);
                mAudioDecoder.configure(format, null, null, 0);
                mAudioDecoder.start();
            }
        }
    }

    private void initReadPacket() {
        mReadPacketRunnable = new Runnable() {
            @Override
            public void run() {
                int inputBufIndex;
                boolean videoReadDone = false;
                boolean audioReadDone = false;
                mVideoDecodeInputBuffer = mVideoDecoder.getInputBuffers();
                mAudioDecodeInputBuffer = mAudioDecoder.getInputBuffers();
                while (!videoReadDone || !audioReadDone) {
                    if (!videoReadDone) {
                        inputBufIndex = mVideoDecoder.dequeueInputBuffer(DECODE_TIMEOUT);
                        if (inputBufIndex >= 0) {
                            //todo 切换流后读取的位置不知道会不会重置，若重置就不能共用一个Extractor
//                            mVideoMediaExtractor.unselectTrack(mAudioTrackIndex);
                            mVideoMediaExtractor.selectTrack(mVideoTrackIndex);
                            if (mVideoMediaExtractor.getSampleTrackIndex() != mVideoTrackIndex) {
                                Log.e(TAG, "无法选择视频轨道");
                                continue;
                            }
                            ByteBuffer inputBuf = mVideoDecodeInputBuffer[inputBufIndex];
                            inputBuf.clear();
                            int sampleSize = mVideoMediaExtractor.readSampleData(inputBuf, 0);
                            if (sampleSize > 0) {
                                mVideoDecoder.queueInputBuffer(inputBufIndex, 0,
                                        sampleSize, mVideoMediaExtractor.getSampleTime(), 0);
                                mVideoMediaExtractor.advance();
                                Log.d(TAG, "视频流 queueInputBuffer");
                            } else {
                                mVideoDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                videoReadDone = true;
                                Log.d(TAG, "视频流已读完");
                            }
                        } else {
//                            Log.d(TAG, "视频解码器没有可用的 inputBuffer");
                        }
                    }

                    audioReadDone = true;

//                    if (!audioReadDone) {
//                        inputBufIndex = mAudioDecoder.dequeueInputBuffer(DECODE_TIMEOUT);
//                        if (inputBufIndex >= 0) {
//                            mVideoMediaExtractor.unselectTrack(mVideoTrackIndex);
//                            mVideoMediaExtractor.selectTrack(mAudioTrackIndex);
//                            if (mVideoMediaExtractor.getSampleTrackIndex() != mAudioTrackIndex) {
//                                Log.e(TAG, "无法选择音频轨道");
//                                continue;
//                            }
//                            ByteBuffer inputBuf = mAudioDecodeInputBuffer[inputBufIndex];
//                            inputBuf.clear();
//                            int sampleSize = mVideoMediaExtractor.readSampleData(inputBuf, 0);
//                            if (sampleSize > 0) {
//                                mAudioDecoder.queueInputBuffer(inputBufIndex, 0,
//                                        sampleSize, mVideoMediaExtractor.getSampleTime(), 0);
//                                mVideoMediaExtractor.advance();
//                                Log.d(TAG, "音频流 queueInputBuffer");
//                            } else {
//                                mAudioDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
//                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                                audioReadDone = true;
//                                Log.d(TAG, "音频流已读完");
//                            }
//                        } else {
//                            Log.d(TAG, "音频解码器没有可用的 inputBuffer");
//                        }
//                    }
                }
            }
        };
        new Thread(mReadPacketRunnable).start();
    }

    // 渲染必须在mCodecOutputSurface初始化的线程
    private void initVideo() {
        boolean decodeDone = false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (!decodeDone) {
            int decoderStatus = mVideoDecoder.dequeueOutputBuffer(bufferInfo, DECODE_TIMEOUT);
            if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.d(TAG, "没有解码后的视频帧");
            } else if (decoderStatus < 0) {
                Log.d(TAG, "decoderStatus < 0");
            } else {
                mVideoDecoder.releaseOutputBuffer(decoderStatus, bufferInfo.size != 0);
                if (bufferInfo.size > 0) {
                    Log.d(TAG, "视频帧解码完一帧");
                    mCodecOutputSurface.awaitNewImage();
                    mCodecOutputSurface.drawImage(true);
//todo dequeueOutputBuffer  writeSampleData，然后 swapBuffers
                    mCodecOutputSurface.setPresentationTime(bufferInfo.presentationTimeUs);
                    mCodecOutputSurface.swapBuffers();
                } else {
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.d(TAG, "视频帧解码完毕");
                        decodeDone = true;
                    }
                }
            }
        }
    }


    private SaveTask(VideoSaveInfo videoSaveInfo, SaveFilters saveFilters) {
        mVideoSaveInfo = videoSaveInfo;
        mSaveFilters = saveFilters;
    }

    public static void save(VideoSaveInfo v, SaveFilters saveFilters) {
        new Thread(new SaveTask(v, saveFilters)).start();
    }

    @Override
    public void run() {
        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initReadPacket();
        initVideo();
    }


    private boolean mixBgMusic() {
        return mSaveFilters != null && mSaveFilters.getBgMusicInfo() != null;
    }


}
