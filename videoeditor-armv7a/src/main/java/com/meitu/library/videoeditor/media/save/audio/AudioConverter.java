package com.meitu.library.videoeditor.media.save.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.meitu.library.videoeditor.media.save.SaveFilters;
import com.meitu.library.videoeditor.media.save.muxer.MuxStore;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by wyh3 on 2018/3/25.
 * 音频流处理
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AudioConverter {
    private final static String TAG = Tag.build("AudioConverter");
    private final static String ENCODE_MIME = "audio/mp4a-latm";
    private static final int DECODE_TIMEOUT = 1000;
    private static final int ENCODE_TIMEOUT = 1000;
    private ArrayBlockingQueue<PcmData> mAudioPcmQueue;
    private ArrayBlockingQueue<PcmData> mBgMusicPcmQueue;
    private ArrayBlockingQueue<PcmData> mMixPcmQueue;
    private MediaCodec mDecoder;
    private MediaCodec mEncoder;
    private boolean mDecodeDone = false;
    private MediaExtractor mExtractor;
    private int mTrackIndex = -1;
    private int mMuxTrackIndex = -1;
    private MuxStore mMuxStore;
    private VideoSaveInfo mVideoSaveInfo;
    private SaveFilters mSaveFilters;
    private boolean mMixFlag;
    private boolean mDecEncFlag;

    private Runnable mCopyTrackRunnable = new Runnable() {
        @Override
        public void run() {
            copyTrack();
        }
    };


    private Runnable mDecodeRunnable = new Runnable() {
        @Override
        public void run() {
            decode();
        }
    };

    private Runnable mBgMusicDecodeRunnable = new Runnable() {
        @Override
        public void run() {
            bgMusicDecode();
        }
    };


    private Runnable mMixRunnable = new Runnable() {
        @Override
        public void run() {
            mix();
        }
    };


    private Runnable mEncodeRunnable = new Runnable() {
        @Override
        public void run() {
            encode();
            release();
        }
    };

    private void copyTrack() {
        boolean copyDone = false;
        ByteBuffer buffer = ByteBuffer.allocate(1024*64);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        mExtractor.selectTrack(mTrackIndex);
        mMuxTrackIndex = mMuxStore.addTrack(mExtractor.getTrackFormat(mTrackIndex));
        //todo 多线程同时写入会有问题
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        while (!copyDone) {
            int length = mExtractor.readSampleData(buffer, 0);
            if (length != -1) {
                Log.d(TAG, "copyTrack: addData");
                int flags = mExtractor.getSampleFlags();
                bufferInfo.size = length;
                bufferInfo.flags = flags;
                bufferInfo.presentationTimeUs = mExtractor.getSampleTime();
                bufferInfo.offset = 0;
                mMuxStore.addData(mMuxTrackIndex, buffer, bufferInfo);
                mExtractor.advance();
            } else {
                copyDone = true;
                Log.d(TAG, "copyTrack: copyDone");
            }
        }
        buffer.clear();
    }


    private void initMix() {
    }

    private void initBgEncoder() {
    }

    private void initDecoder() throws IOException {
        MediaFormat format = mExtractor.getTrackFormat(mTrackIndex);
        String mime = format.getString(MediaFormat.KEY_MIME);
        mDecoder = MediaCodec.createDecoderByType(mime);
        Log.d(TAG, "Decoder configure:" + format);
        mDecoder.configure(format, null, null, 0);
        mDecoder.start();
//        MediaFormat mediaFormat = new MediaFormat();
//        //数据类型
//        mediaFormat.setString(MediaFormat.KEY_MIME, mime);
//        //声道个数
//        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
//        //采样率
//        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000);
//        //比特率
//        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
//        //用来标记AAC是否有adts头，1->有
////                mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
//        //用来标记aac的类型
//        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//        //ByteBuffer key（暂时不了解该参数的含义，但必须设置）
//        byte[] data = new byte[]{(byte) 0x11, (byte) 0x90};
//        ByteBuffer csd_0 = ByteBuffer.wrap(data);
//        mediaFormat.setByteBuffer("csd-0", csd_0);
//
//        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 10 * 1024);//作用于inputBuffer的大小
    }

    private void initEncoder() throws IOException {
        MediaFormat format = MediaFormat.createAudioFormat(ENCODE_MIME,
                48000, 2);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 10 * 1024);//作用于inputBuffer的大小
        mEncoder = MediaCodec.createEncoderByType(ENCODE_MIME);
        mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();
    }

    private void initExtractor() throws IOException {
        mExtractor = new MediaExtractor();
        mExtractor.setDataSource(mVideoSaveInfo.getSrcPath());
        for (int i = 0; i < mExtractor.getTrackCount(); i++) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            Log.d(TAG, "getTrackFormat index=" + i + " (" + mime + "): " + format);
            if (mime.startsWith("audio/")) {
                mTrackIndex = i;
            }
        }
    }


    private void decode() {
        int bufIndex;
        boolean readDone = false;
        ByteBuffer[] decodeInputBuffer = mDecoder.getInputBuffers();
        ByteBuffer[] decodeOutputBuffer = mDecoder.getOutputBuffers();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        mExtractor.selectTrack(mTrackIndex);
        while (true) {
            if (!readDone) {
                bufIndex = mDecoder.dequeueInputBuffer(DECODE_TIMEOUT);
                if (bufIndex >= 0) {
                    ByteBuffer inputBuf = decodeInputBuffer[bufIndex];
                    inputBuf.clear();
                    int sampleSize = mExtractor.readSampleData(inputBuf, 0);
                    if (sampleSize > 0) {
                        mDecoder.queueInputBuffer(bufIndex, 0,
                                sampleSize, mExtractor.getSampleTime(), 0);
                        mExtractor.advance();
                        Log.d(TAG, "音频流 queueInputBuffer");
                    } else {
                        mDecoder.queueInputBuffer(bufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        readDone = true;
                        Log.d(TAG, "音频流已读完");
                    }
                }
            }
            bufIndex = mDecoder.dequeueOutputBuffer(bufferInfo, DECODE_TIMEOUT);
            if (bufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (bufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                decodeOutputBuffer = mDecoder.getOutputBuffers();
            } else if (bufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mDecoder.getOutputFormat();
                Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED : " + newFormat);
            } else if (bufIndex < 0) {
                Log.d(TAG, "mVideoDecoder bufIndex < 0");
            } else {
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "mVideoDecoder BUFFER_FLAG_END_OF_STREAM");
                    mDecodeDone = true;
                    break;
                }
                if (bufferInfo.size != 0) {
                    Log.d(TAG, "音频帧解码完一帧");
                    ByteBuffer byteBuffer = decodeOutputBuffer[bufIndex];
                    byte[] data = new byte[bufferInfo.size];
                    byteBuffer.get(data);
                    byteBuffer.clear();
                    putPcmData(mAudioPcmQueue, new PcmData(data, bufferInfo.presentationTimeUs));
                }
                mDecoder.releaseOutputBuffer(bufIndex, false);

            }
        }
    }

    private void encode() {
        boolean readPcmDone = false;
        ByteBuffer[] encodeInputBuffers = mEncoder.getInputBuffers();
        ByteBuffer[] encodeOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            if (!readPcmDone) {
                int bufIndex = mEncoder.dequeueInputBuffer(ENCODE_TIMEOUT);
                if (bufIndex > 0) {
                    PcmData pcmData = pollPcmData(mAudioPcmQueue);
                    ByteBuffer inputBuffer = encodeInputBuffers[bufIndex];
                    inputBuffer.clear();
                    if (pcmData == null) {
                        if (mDecodeDone) {
                            Log.d(TAG, "读取完pcm了");
                            mEncoder.queueInputBuffer(bufIndex, 0, 0,
                                    0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            readPcmDone = true;
                        } else {
                            Log.d(TAG, "编码线程未从缓冲队列中读到帧数据");
                        }
                    } else {
                        inputBuffer.limit(pcmData.data.length);
                        inputBuffer.put(pcmData.data);
                        Log.d(TAG, "把pcm数据加入编码队列");
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
                mMuxTrackIndex = mMuxStore.addTrack(newFormat);
            } else if (encodeStatus < 0) {
            } else {
                ByteBuffer encodedData = encodeOutputBuffers[encodeStatus];
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size > 0) {
                    Log.d(TAG, "写入音频数据");
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);
                    bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                    Log.d(TAG, "buf.size=" + bufferInfo.size + " presentationTimeUs=" + bufferInfo.presentationTimeUs + " flags=" +
                            bufferInfo.flags + " offset=" + bufferInfo.offset);
                    mMuxStore.addData(mMuxTrackIndex, encodedData, bufferInfo);
                }
                mEncoder.releaseOutputBuffer(encodeStatus, false);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "写完音频数据");
                    break;
                }
            }
        }
    }

    private void bgMusicDecode() {

    }

    private void mix() {
    }

    private void putPcmData(ArrayBlockingQueue<PcmData> queue, PcmData pcmData) {
        try {
            queue.put(pcmData);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private PcmData pollPcmData(ArrayBlockingQueue<PcmData> queue) {
        PcmData data = null;
        try {
            data = queue.poll(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return data;
    }


    public void release() {

        mExtractor.release();
        mExtractor = null;

        if (mDecEncFlag) {
            mDecoder.stop();
            mDecoder.release();
            mDecoder = null;
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }

        if (mMixFlag) {

        }

    }

    public void run(ExecutorService executors) {
        if (mDecEncFlag) {
            Log.d(TAG, "run: DecEnc");
            executors.execute(mDecodeRunnable);
            executors.execute(mEncodeRunnable);
            if (mMixFlag) {
                Log.d(TAG, "run: mix");

            }
        } else {
            Log.d(TAG, "run: CopyTrack");

            executors.execute(mCopyTrackRunnable);
        }
    }

    public AudioConverter(VideoSaveInfo videoSaveInfo, SaveFilters saveFilters, MuxStore muxStore) {
        mVideoSaveInfo = videoSaveInfo;
        mSaveFilters = saveFilters;
        mMuxStore = muxStore;
        mMixFlag = mSaveFilters.getBgMusicInfo() != null;
        mDecEncFlag = mMixFlag || mVideoSaveInfo.getVideoVolume() != 1;
        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void prepare() throws IOException {
        initExtractor();
        if (mDecEncFlag) {
            mAudioPcmQueue = new ArrayBlockingQueue<>(10);
            initEncoder();
            initDecoder();
            if (mMixFlag) {
                mBgMusicPcmQueue = new ArrayBlockingQueue<>(10);
                mMixPcmQueue = new ArrayBlockingQueue<>(10);
                initBgEncoder();
                initMix();
            }
        }
    }
}
