package com.meitu.library.videoeditor.save.video;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import com.meitu.library.videoeditor.save.bean.SaveFilters;
import com.meitu.library.videoeditor.save.muxer.MuxStore;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

/**
 * Created by wyh3 on 2018/3/25.
 * 视频流处理
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class VideoConverter implements Runnable {
    private final static String TAG = Tag.build("VideoConverter");
    private final static String ENCODE_MIME = "video/avc";
    private static final int DECODE_TIMEOUT = 1000;
    private static final int ENCODE_TIMEOUT = 1000;
    private static final int notifyAudioCount = 1; //写入一定数量视频帧后再开始写音频帧，否则音频断断续续
    private int mWriteCount = 0;
    private final Object VideoWroteLock;
    public static boolean videoWrote = false;
    private MediaCodec mDecoder;
    private MediaCodec mEncoder;
    private CodecOutputSurface mCodecOutputSurface;
    private MediaExtractor mExtractor;
    private MuxStore mMuxStore;
    private int mTrackIndex = -1;
    private int mMuxerTrackIndex = -1;
    private VideoSaveInfo mVideoSaveInfo;
    private SaveFilters mSaveFilters;



    public VideoConverter(VideoSaveInfo videoSaveInfo, SaveFilters saveFilters, MuxStore muxStore, Object videoWroteLock) {
        this.VideoWroteLock = videoWroteLock;
        mVideoSaveInfo = videoSaveInfo;
        mSaveFilters = saveFilters;
        mMuxStore = muxStore;
    }

    @Override
    public void run() {
        long t = System.currentTimeMillis();
        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        extract();
        release();
        long time = System.currentTimeMillis() - t;
        Log.d(TAG, "save video cost " + time + " ms");
    }

    public void prepare() throws IOException {

        MediaFormat videoEncodeFormat = MediaFormat.createVideoFormat(ENCODE_MIME,
                mVideoSaveInfo.getOutputWidth(), mVideoSaveInfo.getOutputHeight());
        videoEncodeFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        videoEncodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoSaveInfo.getOutputBitrate());
        videoEncodeFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mVideoSaveInfo.getFps());
        videoEncodeFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mVideoSaveInfo.getIFrameInterval());
        mEncoder = MediaCodec.createEncoderByType(ENCODE_MIME);
        mEncoder.configure(videoEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        Surface encodeSurface = mEncoder.createInputSurface();
        mEncoder.start();

        mExtractor = new MediaExtractor();
        mExtractor.setDataSource(mVideoSaveInfo.getSrcPath());

        for (int i = 0; i < mExtractor.getTrackCount(); i++) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            Log.d(TAG, "getTrackFormat index=" + i + " (" + mime + "): " + format);
            if (mime.startsWith("video/")) {
                mTrackIndex = i;
                mDecoder = MediaCodec.createDecoderByType(mime);
                mCodecOutputSurface = new CodecOutputSurface(
                        mVideoSaveInfo.getOutputWidth(), mVideoSaveInfo.getOutputHeight(),
                        encodeSurface, mSaveFilters.isFilter());
                mDecoder.configure(format, mCodecOutputSurface.getSurface(), null, 0);
                mDecoder.start();
            }
        }
    }

    public void extract() {
        int bufIndex;
        boolean readDone = false;
        boolean writeDone = false;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        ByteBuffer[] videoDecodeInputBuffer = mDecoder.getInputBuffers();
        mExtractor.selectTrack(mTrackIndex);

        while (!writeDone) {
            if (!readDone) {
                bufIndex = mDecoder.dequeueInputBuffer(DECODE_TIMEOUT);
                if (bufIndex >= 0) {
                    ByteBuffer inputBuf = videoDecodeInputBuffer[bufIndex];
                    int sampleSize = mExtractor.readSampleData(inputBuf, 0);
                    if (sampleSize > 0) {
                        mDecoder.queueInputBuffer(bufIndex, 0,
                                sampleSize, mExtractor.getSampleTime(), 0);
                        mExtractor.advance();
                        Log.d(TAG, "视频流 queueInputBuffer");
                    } else {
                        mDecoder.queueInputBuffer(bufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        readDone = true;
                        Log.d(TAG, "视频流已读完");
                    }
                }
            }
            bufIndex = mDecoder.dequeueOutputBuffer(bufferInfo, DECODE_TIMEOUT);
            if (bufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.d(TAG, "mVideoDecoder INFO_TRY_AGAIN_LATER");
            } else if (bufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                // not important for us, since we're using Surface
                Log.d(TAG, "mVideoDecoder INFO_OUTPUT_BUFFERS_CHANGED");
            } else if (bufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mDecoder.getOutputFormat();
                Log.d(TAG, "mVideoDecoder INFO_OUTPUT_FORMAT_CHANGED : " + newFormat);
            } else if (bufIndex < 0) {
                Log.d(TAG, "mVideoDecoder bufIndex < 0");
            } else {
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "mVideoDecoder BUFFER_FLAG_END_OF_STREAM");
                    writeDone = true;
                }
                mDecoder.releaseOutputBuffer(bufIndex, bufferInfo.size != 0);
                if (bufferInfo.size != 0) {
                    // 渲染必须在mCodecOutputSurface初始化的线程
                    mCodecOutputSurface.awaitNewImage();
                    mCodecOutputSurface.drawImage(true);
                    writeVideoData(false);
                    mCodecOutputSurface.setPresentationTime(bufferInfo.presentationTimeUs * 1000);
                    mCodecOutputSurface.swapBuffers();
                }
            }
        }
        writeVideoData(true);
    }


    private void writeVideoData(boolean endOfStream) {
        Log.d(TAG, "writeVideoData endOfStream=" + endOfStream);
        if (endOfStream) {
            mEncoder.signalEndOfInputStream();
        }
        ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (true) {
            int bufIndex = mEncoder.dequeueOutputBuffer(bufferInfo, ENCODE_TIMEOUT);
            if (bufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (endOfStream) {
                    Log.d(TAG, "no output available, spinning to await EOS");
                } else {
                    break;
                }
            } else if (bufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = mEncoder.getOutputBuffers();
            } else if (bufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mMuxerTrackIndex = mMuxStore.addTrack(mEncoder.getOutputFormat());
            } else if (bufIndex < 0) {
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[bufIndex];
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG ,mBufferInfo.size=" + bufferInfo.size);
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0) {
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);
                    Log.d(TAG, "writeSampleData: addData size=" + bufferInfo.size +
                            " flags=" + bufferInfo.flags + " presentationTimeUs=" +
                            bufferInfo.presentationTimeUs);
                    mMuxStore.addData(mMuxerTrackIndex, encodedData, bufferInfo);
                    if (!videoWrote) {
                        mWriteCount++;
                        if (mWriteCount == notifyAudioCount) {
                            synchronized (VideoWroteLock) {
                                videoWrote = true;
                                VideoWroteLock.notifyAll();
                            }
                        }
                    }
                }
                mEncoder.releaseOutputBuffer(bufIndex, false);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    if (!endOfStream) {
                        Log.w(TAG, "reached end of stream unexpectedly");
                    } else {
                        Log.d(TAG, "end of stream reached");
                    }
                    if (!videoWrote) {
                        synchronized (VideoWroteLock) {
                            videoWrote = true;
                            VideoWroteLock.notifyAll();
                        }
                    }
                    break;
                }
            }
        }

    }

    public void release() {
        Log.d(TAG, "release");
        mDecoder.stop();
        mDecoder.release();
        mDecoder = null;

        mExtractor.release();
        mExtractor = null;

        mCodecOutputSurface.release();
        mCodecOutputSurface = null;

        mEncoder.stop();
        mEncoder.release();
        mEncoder = null;

        videoWrote = false;

    }

    public void run(ExecutorService executors) {
        executors.execute(this);
    }
}
