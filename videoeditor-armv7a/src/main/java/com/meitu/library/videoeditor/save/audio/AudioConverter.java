package com.meitu.library.videoeditor.save.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

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
    private static final int AUDIO_DECODE_MAX_INPUT_SIZE = 1024;
    private static final int BG_MUSIC_DECODE_MAX_INPUT_SIZE = 1024;
    private final Object VideoWroteLock;
    private ArrayBlockingQueue<PcmData> mAudioPcmQueue;
    private ArrayBlockingQueue<PcmData> mBgMusicPcmQueue;
    private ArrayBlockingQueue<PcmData> mMixPcmQueue;
    private MediaCodec mDecoder;
    private MediaCodec mEncoder;
    private MediaCodec mBgMusicDecoder;
    private boolean[] mDecodeDone = new boolean[]{false};
    private boolean[] mBgMusicDecodeDone = new boolean[]{false};
    private boolean[] mHandleDone = new boolean[]{false};
    private MediaExtractor mExtractor;
    private MediaExtractor mBgMusicExtractor;
    private int mTrackIndex = -1;
    private int mBgMusicTrackIndex = -1;
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
            decode(mDecoder, mExtractor, mTrackIndex,
                    mAudioPcmQueue, mDecodeDone);
        }
    };

    private Runnable mBgMusicDecodeRunnable = new Runnable() {
        @Override
        public void run() {
            decode(mBgMusicDecoder, mBgMusicExtractor, mBgMusicTrackIndex,
                    mBgMusicPcmQueue, mBgMusicDecodeDone);
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
            if (mMixFlag) {
                encode(mMixPcmQueue);
            } else {
                encode(mAudioPcmQueue);
            }
            release();
        }
    };

    private boolean isEncodeReadDone() {
        return mMixFlag ? mHandleDone[0] : mDecodeDone[0];
    }

    private void copyTrack() {
        boolean copyDone = false;
        ByteBuffer buffer = ByteBuffer.allocate(1024 * 64);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        mExtractor.selectTrack(mTrackIndex);
        mMuxTrackIndex = mMuxStore.addTrack(mExtractor.getTrackFormat(mTrackIndex));
        waitVideoWrote();
        while (!copyDone) {
            buffer.clear();
            int length = mExtractor.readSampleData(buffer, 0);
            if (length != -1) {
                int flags = mExtractor.getSampleFlags();
                bufferInfo.size = length;
                bufferInfo.flags = flags;
                bufferInfo.presentationTimeUs = mExtractor.getSampleTime();
                bufferInfo.offset = 0;
                Log.d(TAG, "copyTrack: addData size=" + length + " flags=" + flags + " presentationTimeUs=" +
                        bufferInfo.presentationTimeUs);
                mMuxStore.addData(mMuxTrackIndex, buffer, bufferInfo);
                mExtractor.advance();
            } else {
                copyDone = true;
                bufferInfo.size = 0;
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                Log.d(TAG, "copyTrack: addData size=" + length + " flags=" + bufferInfo.flags + " presentationTimeUs=" +
                        bufferInfo.presentationTimeUs);
                mMuxStore.addData(mMuxTrackIndex, buffer, bufferInfo);
                Log.d(TAG, "copyTrack: copyDone");
            }
        }
    }

    private void waitVideoWrote() {
        synchronized (VideoWroteLock) {
            while (!VideoConverter.videoWrote) {
                try {
                    VideoWroteLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private void initBgMusicDecoder() throws IOException {
        MediaFormat format = mBgMusicExtractor.getTrackFormat(mBgMusicTrackIndex);
        String mime = format.getString(MediaFormat.KEY_MIME);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BG_MUSIC_DECODE_MAX_INPUT_SIZE);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000);
        mBgMusicDecoder = MediaCodec.createDecoderByType(mime);
        Log.d(TAG, "initBgMusicDecoder: format:" + format);
        mBgMusicDecoder.configure(format, null, null, 0);
        mBgMusicDecoder.start();
    }

    private void initDecoder() throws IOException {
        MediaFormat format = mExtractor.getTrackFormat(mTrackIndex);
        String mime = format.getString(MediaFormat.KEY_MIME);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_DECODE_MAX_INPUT_SIZE);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 48000);
        mDecoder = MediaCodec.createDecoderByType(mime);
        Log.d(TAG, "initDecoder: format:" + format);
        mDecoder.configure(format, null, null, 0);
        mDecoder.start();
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
            if (mime.startsWith("audio/")) {
                Log.d(TAG, "initExtractor index=" + i + " (" + mime + "): " + format);
                mTrackIndex = i;
            }
        }
    }

    private void initBgMusicExtractor() throws IOException {
        mBgMusicExtractor = new MediaExtractor();
        mBgMusicExtractor.setDataSource(mSaveFilters.getBgMusicInfo().getMusicPath());
        for (int i = 0; i < mBgMusicExtractor.getTrackCount(); i++) {
            MediaFormat format = mBgMusicExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                mBgMusicTrackIndex = i;
                Log.d(TAG, "initBgMusicExtractor index=" + i + " (" + mime + "): " + format);
            }
        }
    }

    private void decode(MediaCodec decoder, MediaExtractor extractor,
                        int trackIndex, ArrayBlockingQueue<PcmData> queue,
                        boolean[] decodeDone) {
        int bufIndex;
        boolean readDone = false;
        ByteBuffer[] decodeInputBuffer = decoder.getInputBuffers();
        ByteBuffer[] decodeOutputBuffer = decoder.getOutputBuffers();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        extractor.selectTrack(trackIndex);
        while (true) {
            if (!readDone) {
                bufIndex = decoder.dequeueInputBuffer(DECODE_TIMEOUT);
                if (bufIndex >= 0) {
                    ByteBuffer inputBuf = decodeInputBuffer[bufIndex];
                    inputBuf.clear();
                    int sampleSize = extractor.readSampleData(inputBuf, 0);
                    if (sampleSize > 0) {
                        decoder.queueInputBuffer(bufIndex, 0,
                                sampleSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                        Log.d(TAG, "音频流 queueInputBuffer");
                    } else {
                        decoder.queueInputBuffer(bufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        readDone = true;
                        Log.d(TAG, "音频流已读完");
                    }
                }
            }
            bufIndex = decoder.dequeueOutputBuffer(bufferInfo, DECODE_TIMEOUT);
            if (bufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (bufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                decodeOutputBuffer = decoder.getOutputBuffers();
            } else if (bufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = decoder.getOutputFormat();
                Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED : " + newFormat);
            } else if (bufIndex < 0) {
                Log.d(TAG, "mVideoDecoder bufIndex < 0");
            } else {
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "mVideoDecoder BUFFER_FLAG_END_OF_STREAM");
                    decodeDone[0] = true;
                    break;
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Log.d(TAG, "mVideoDecoder BUFFER_FLAG_CODEC_CONFIG");
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0) {
                    Log.d(TAG, "音频帧解码完一帧");
                    ByteBuffer byteBuffer = decodeOutputBuffer[bufIndex];
                    byte[] data = new byte[bufferInfo.size];
                    byteBuffer.get(data);
                    byteBuffer.clear();
                    putPcmData(queue, new PcmData(data, bufferInfo.presentationTimeUs));
                }
                decoder.releaseOutputBuffer(bufIndex, false);
            }
        }
    }


    private void mix() {
        while (true) {
            PcmData audioPcmData = pollPcmData(mAudioPcmQueue);
            PcmData bgMusicPcmData = pollPcmData(mBgMusicPcmQueue);
            if (audioPcmData == null && mDecodeDone[0]) {
                Log.d(TAG, "mix: 原音读取完毕");
                break;
            }
            if (bgMusicPcmData == null && mBgMusicDecodeDone[0]) {
                Log.d(TAG, "mix: 背景音乐读取完毕");
                break;
            }
            Log.d(TAG, "mix: audioPcmData length=" + audioPcmData.data.length +
                    " pts=" + audioPcmData.pts +
                    " bgMusicPcmData length=" + bgMusicPcmData.data.length +
                    " pts=" + bgMusicPcmData.pts
            );
            byte[][] allAudioBytes = new byte[2][];
            allAudioBytes[0] = bgMusicPcmData.data;
            allAudioBytes[1] = audioPcmData.data;
            //todo 采样率、数据量不同 如何混音 ？
//            byte[] mixVideo = AudioMix.normalizationMix(allAudioBytes);
//            putPcmData(mMixPcmQueue, new PcmData(mixVideo, audioPcmData.pts));

            //暂时把背景音乐写进去
            putPcmData(mMixPcmQueue, new PcmData(bgMusicPcmData.data, bgMusicPcmData.pts));
        }
        mHandleDone[0] = true;
    }


    private void encode(ArrayBlockingQueue<PcmData> queue) {
        boolean readPcmDone = false;
        ByteBuffer[] encodeInputBuffers = mEncoder.getInputBuffers();
        ByteBuffer[] encodeOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            if (!readPcmDone) {
                int bufIndex = mEncoder.dequeueInputBuffer(ENCODE_TIMEOUT);
                if (bufIndex > 0) {
                    PcmData pcmData = pollPcmData(queue);
                    ByteBuffer inputBuffer = encodeInputBuffers[bufIndex];
                    inputBuffer.clear();
                    if (pcmData == null) {
                        if (isEncodeReadDone()) {
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
                waitVideoWrote();
            } else if (encodeStatus < 0) {
            } else {
                ByteBuffer byteBuffer = encodeOutputBuffers[encodeStatus];
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "写完音频数据");
                    break;
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size > 0) {
                    Log.d(TAG, "写入音频数据");
                    byteBuffer.position(bufferInfo.offset);
                    byteBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    Log.d(TAG, VELog.toString(bufferInfo));
                    mMuxStore.addData(mMuxTrackIndex, byteBuffer, bufferInfo);
                }
                mEncoder.releaseOutputBuffer(encodeStatus, false);
            }
        }
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
            mBgMusicDecoder.stop();
            mBgMusicDecoder.release();
            mBgMusicDecoder = null;
            mBgMusicExtractor.release();
            mBgMusicExtractor = null;
        }

    }

    public void run(ExecutorService executors) {
        if (mDecEncFlag) {
            Log.d(TAG, "run: DecEnc");
            executors.execute(mDecodeRunnable);
            executors.execute(mEncodeRunnable);
            if (mMixFlag) {
                Log.d(TAG, "run: mix");
                executors.execute(mBgMusicDecodeRunnable);
                executors.execute(mMixRunnable);
            }
        } else {
            Log.d(TAG, "run: CopyTrack");
            executors.execute(mCopyTrackRunnable);
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
                initBgMusicExtractor();
                initBgMusicDecoder();
            }
        }
    }


    public AudioConverter(VideoSaveInfo videoSaveInfo, SaveFilters saveFilters, MuxStore muxStore, Object videoWroteLock) {
        mVideoSaveInfo = videoSaveInfo;
        mSaveFilters = saveFilters;
        mMuxStore = muxStore;
        this.VideoWroteLock = videoWroteLock;
        mMixFlag = mSaveFilters.getBgMusicInfo() != null;
        mDecEncFlag = mMixFlag || mVideoSaveInfo.getVideoVolume() != 1;
        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
