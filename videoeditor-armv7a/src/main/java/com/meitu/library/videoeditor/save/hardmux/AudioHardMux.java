package com.meitu.library.videoeditor.save.hardmux;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.meitu.library.videoeditor.save.bean.AVData;
import com.meitu.library.videoeditor.save.bean.AVDataUtil;
import com.meitu.library.videoeditor.save.bean.SaveFilters;
import com.meitu.library.videoeditor.save.muxer.MuxStore;
import com.meitu.library.videoeditor.save.util.VELog;
import com.meitu.library.videoeditor.save.video.VideoConverter;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 * Created by wyh3 on 2018/3/30.
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AudioHardMux implements Runnable {

    private final static String TAG = Tag.build("AudioHardMux");
    private final static String ENCODE_MIME = "audio/mp4a-latm";
    private static final int ENCODE_TIMEOUT = 1000;
    private static final int ENCODE_SAMPLE_RATE = 44100;
    private static final int ENCODE_BIT_RATE = 96000;
    private static final int ENCODE_MAX_INPUT_SIZE = 1024 * 12;
    private final Object VideoWroteLock;
    private int mMuxerTrackIndex = -1;
    private MuxStore mMuxStore;
    private MediaCodec mEncoder;
    private VideoSaveInfo mVideoSaveInfo;
    private SaveFilters mSaveFilters;
    private volatile boolean mDecodeDone;
    private ArrayBlockingQueue<AVData> mAudioPcmQueue = new ArrayBlockingQueue<>(30);


    public AudioHardMux(VideoSaveInfo videoSaveInfo, SaveFilters saveFilters, MuxStore muxStore, Object videoWroteLock) {
        mVideoSaveInfo = videoSaveInfo;
        mSaveFilters = saveFilters;
        mMuxStore = muxStore;
        this.VideoWroteLock = videoWroteLock;
    }

    private void waitVideoWrote() {
        synchronized (VideoWroteLock) {
            while (!VideoHardMux.videoWrote) {
                try {
                    VideoWroteLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d(TAG, "waitVideoWrote: 解锁");
        }
    }

    private void encode() {
        boolean readDone = false;
        ByteBuffer[] encodeInputBuffers = mEncoder.getInputBuffers();
        ByteBuffer[] encodeOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            if (!readDone) {
                int bufIndex = mEncoder.dequeueInputBuffer(ENCODE_TIMEOUT);
                if (bufIndex > 0) {
                    AVData pcmData = AVDataUtil.pollAVData(mAudioPcmQueue);
                    ByteBuffer inputBuffer = encodeInputBuffers[bufIndex];
                    inputBuffer.clear();
                    if (pcmData == null) {
                        if (mDecodeDone) {
                            Log.d(TAG, "encode 读取完pcm了");
                            mEncoder.queueInputBuffer(bufIndex, 0, 0,
                                    0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            readDone = true;
                        } else {
                            Log.d(TAG, "encode 编码线程未从缓冲队列中读到帧数据");
                        }
                    } else {
                        inputBuffer.limit(pcmData.data.length);
                        inputBuffer.put(pcmData.data);
                        Log.d(TAG, "encode 把pcm数据加入编码队列 pcmData.pts=" + pcmData.pts);
                        mEncoder.queueInputBuffer(bufIndex, 0, pcmData.data.length,
                                pcmData.pts, 0);
                    }
                }
            }
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int encodeStatus = mEncoder.dequeueOutputBuffer(bufferInfo, ENCODE_TIMEOUT);
            if (encodeStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (encodeStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encodeOutputBuffers = mEncoder.getOutputBuffers();
            } else if (encodeStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mEncoder.getOutputFormat();
                Log.d(TAG, "encode: INFO_OUTPUT_FORMAT_CHANGED " + newFormat);
                mMuxerTrackIndex = mMuxStore.addTrack(newFormat);
                waitVideoWrote();
            } else if (encodeStatus < 0) {
            } else {
                ByteBuffer byteBuffer = encodeOutputBuffers[encodeStatus];
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "encode 写完音频数据");
                    break;
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size > 0) {
                    byteBuffer.position(bufferInfo.offset);
                    byteBuffer.limit(bufferInfo.offset + bufferInfo.size);

                    if (bufferInfo.presentationTimeUs > lastPts) {
                        Log.d(TAG, "写入音频数据" + VELog.toString(bufferInfo));
                        //todo 按照pts递增加入编码队列，返回
                        mMuxStore.addData(mMuxerTrackIndex, byteBuffer, bufferInfo);
                        lastPts = bufferInfo.presentationTimeUs;
                    }

                }
                mEncoder.releaseOutputBuffer(encodeStatus, false);
            }
        }
    }

    private long lastPts=-1;


    private void prepare() throws IOException {
        MediaFormat format = MediaFormat.createAudioFormat(ENCODE_MIME,
                ENCODE_SAMPLE_RATE, 2);
        format.setInteger(MediaFormat.KEY_BIT_RATE, ENCODE_BIT_RATE);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, ENCODE_MAX_INPUT_SIZE);//作用于inputBuffer的大小
        mEncoder = MediaCodec.createEncoderByType(ENCODE_MIME);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();
    }

    private void release() {
        mEncoder.stop();
        mEncoder.release();
        mEncoder = null;

        mAudioPcmQueue.clear();

    }

    public void encode(byte[] data, long pts) {
        Log.d(TAG, "encode: data.length=" + data.length + " pts=" + pts);
        AVDataUtil.putAVData(mAudioPcmQueue, new AVData(data, pts));
    }


    public void run(ExecutorService executors) {
        executors.execute(this);
    }

    @Override
    public void run() {
        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        encode();
        release();
    }


    public void decodeDone() {
        mDecodeDone = true;
    }
}