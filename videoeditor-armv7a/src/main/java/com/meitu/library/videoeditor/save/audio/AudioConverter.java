package com.meitu.library.videoeditor.save.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
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
 * Created by wyh3 on 2018/3/25.
 * 音频流处理
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class AudioConverter {
    private final static String TAG = Tag.build("AudioConverter");
    private final static String ENCODE_MIME = "audio/mp4a-latm";
    private static final int DECODE_TIMEOUT = 1000;
    private static final int ENCODE_TIMEOUT = 1000;
    private static final int AUDIO_ENCODE_TYPE = 1;
    private static final int BGMUSIC_ENCODE_TYPE = 2;
    private static final int AUDIO_DECODE_MAX_INPUT_SIZE = 1024 * 12;
    private static final int BG_MUSIC_DECODE_MAX_INPUT_SIZE = 1024 * 12;
    private static final int COPY_TRACK_READ_SIZE = 1024 * 12;
    private static final int ENCODE_MAX_INPUT_SIZE = 1024 * 12;
    private static final int DECODE_SAMPLE_RATE = 44100;
    private static final int ENCODE_SAMPLE_RATE = 44100;
    private static final int ENCODE_BIT_RATE = 96000;
    private final Object VideoWroteLock;
    private PcmFormat mAudioPcmFormat;
    private PcmDataAlign mPcmDataAlign;
    private ArrayBlockingQueue<AVData> mAudioPcmQueue;
    private ArrayBlockingQueue<AVData> mBgMusicPcmQueue;
    private ArrayBlockingQueue<AVData> mMixPcmQueue;
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
    private volatile boolean mEncodeDone;

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
                    mAudioPcmQueue, mDecodeDone, AUDIO_ENCODE_TYPE);
            Log.d(TAG, "mDecodeRunnable: done");

        }
    };

    private Runnable mBgMusicDecodeRunnable = new Runnable() {
        @Override
        public void run() {
            decode(mBgMusicDecoder, mBgMusicExtractor, mBgMusicTrackIndex,
                    mBgMusicPcmQueue, mBgMusicDecodeDone, BGMUSIC_ENCODE_TYPE);
            Log.d(TAG, "mBgMusicDecodeRunnable: done");
            releaseMix();
        }
    };


    private Runnable mMixRunnable = new Runnable() {
        @Override
        public void run() {
            mix();
            Log.d(TAG, "mMixRunnable: done");
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
        ByteBuffer buffer = ByteBuffer.allocate(COPY_TRACK_READ_SIZE);
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
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, DECODE_SAMPLE_RATE);
        mBgMusicDecoder = MediaCodec.createDecoderByType(mime);
        Log.d(TAG, "initBgMusicDecoder: format:" + format);
        mBgMusicDecoder.configure(format, null, null, 0);
        mBgMusicDecoder.start();
    }

    private void initDecoder() throws IOException {
        MediaFormat format = mExtractor.getTrackFormat(mTrackIndex);
        String mime = format.getString(MediaFormat.KEY_MIME);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, AUDIO_DECODE_MAX_INPUT_SIZE);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, DECODE_SAMPLE_RATE);
        mDecoder = MediaCodec.createDecoderByType(mime);
        Log.d(TAG, "initDecoder: format:" + format);
        mDecoder.configure(format, null, null, 0);
        mDecoder.start();
    }

    private void initEncoder() throws IOException {
        MediaFormat format = MediaFormat.createAudioFormat(ENCODE_MIME,
                ENCODE_SAMPLE_RATE, 2);
        format.setInteger(MediaFormat.KEY_BIT_RATE, ENCODE_BIT_RATE);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, ENCODE_MAX_INPUT_SIZE);//作用于inputBuffer的大小
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
                        int trackIndex, ArrayBlockingQueue<AVData> queue,
                        boolean[] decodeDone, int type) {
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
                    } else {
                        if ((type == BGMUSIC_ENCODE_TYPE) && mSaveFilters.getBgMusicInfo().isLoop()) {
                            extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                        } else {
                            decoder.queueInputBuffer(bufIndex, 0, 0, 0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            readDone = true;
                            Log.d(TAG, type + "音频流已读完");
                        }
                    }
                }
            }
            bufIndex = decoder.dequeueOutputBuffer(bufferInfo, DECODE_TIMEOUT);
            if (bufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (bufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                decodeOutputBuffer = decoder.getOutputBuffers();
            } else if (bufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = decoder.getOutputFormat();
                Log.d(TAG, type + "INFO_OUTPUT_FORMAT_CHANGED : " + newFormat);
            } else if (bufIndex < 0) {
                Log.d(TAG, type + " bufIndex < 0");
            } else {
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, type + " BUFFER_FLAG_END_OF_STREAM");
                    decodeDone[0] = true;
                    break;
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Log.d(TAG, type + " BUFFER_FLAG_CODEC_CONFIG");
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size != 0) {
                    ByteBuffer byteBuffer = decodeOutputBuffer[bufIndex];
                    byte[] data = new byte[bufferInfo.size];
                    byteBuffer.get(data);
                    byteBuffer.clear();
                    AVDataUtil.putAVData(queue, new AVData(data, bufferInfo.presentationTimeUs));
                }
                decoder.releaseOutputBuffer(bufIndex, false);
            }
            if (mEncodeDone) {
                mEncodeDone = false;
                break;
            }
        }
    }

    private void mix() {
        while (true) {
            long[] pts = new long[1];
            byte[][] alignData = new byte[2][];
            int result = mPcmDataAlign.getAlignPcmData(
                    mAudioPcmQueue,
                    mBgMusicPcmQueue,
                    alignData,
                    pts,
                    mDecodeDone,
                    mBgMusicDecodeDone);
            switch (result) {
                case PcmDataAlign.FLAG_AUDIO_POLL_DONE:
                    mHandleDone[0] = true;
                    return;
                case PcmDataAlign.FLAG_BGMUSIC_POLL_DONE:
                    AVDataUtil.putAVData(mMixPcmQueue, new AVData(alignData[0], pts[0]));
                    break;
                case PcmDataAlign.FLAG_ALIGN_DATA:
                    byte[] mixVideo = AudioMix.mixRawAudioBytes(alignData,
                            mVideoSaveInfo.getVideoVolume(),
                            mSaveFilters.getBgMusicInfo().getBgMusicVolume());
                    AVDataUtil.putAVData(mMixPcmQueue, new AVData(mixVideo, pts[0]));
                    break;
                case PcmDataAlign.FLAG_AUDIO_POLL_NULL:
                    break;
            }
        }
    }


    private void encode(ArrayBlockingQueue<AVData> queue) {
        boolean readPcmDone = false;
        ByteBuffer[] encodeInputBuffers = mEncoder.getInputBuffers();
        ByteBuffer[] encodeOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            if (!readPcmDone) {
                int bufIndex = mEncoder.dequeueInputBuffer(ENCODE_TIMEOUT);
                if (bufIndex > 0) {
                    AVData pcmData = AVDataUtil.pollAVData(queue);
                    ByteBuffer inputBuffer = encodeInputBuffers[bufIndex];
                    inputBuffer.clear();
                    if (pcmData == null) {
                        if (isEncodeReadDone()) {
                            Log.d(TAG, "encode 读取完pcm了");
                            mEncoder.queueInputBuffer(bufIndex, 0, 0,
                                    0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            readPcmDone = true;
                        } else {
                            Log.d(TAG, "encode 编码线程未从缓冲队列中读到帧数据");
                        }
                    } else {
                        inputBuffer.limit(pcmData.data.length);
                        inputBuffer.put(pcmData.data);
//                        Log.d(TAG, "encode 把pcm数据加入编码队列");
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
                    Log.d(TAG, "encode 写完音频数据");
                    mEncodeDone = true;
                    break;
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size > 0) {
                    Log.d(TAG, "encode 写入音频数据");
                    byteBuffer.position(bufferInfo.offset);
                    byteBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    Log.d(TAG, VELog.toString(bufferInfo));
                    mMuxStore.addData(mMuxTrackIndex, byteBuffer, bufferInfo);
                }
                mEncoder.releaseOutputBuffer(encodeStatus, false);
            }
        }
    }


    private void releaseMix() {
        if (mMixFlag) {
            mBgMusicDecoder.stop();
            mBgMusicDecoder.release();
            mBgMusicDecoder = null;
            mBgMusicExtractor.release();
            mBgMusicExtractor = null;
            mPcmDataAlign = null;
            mBgMusicPcmQueue.clear();
            mMixPcmQueue.clear();
        }
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

            mAudioPcmQueue.clear();
        }

        if (mMixFlag) {
            mBgMusicPcmQueue.clear();
        }

        long time = System.currentTimeMillis() - t;
        Log.d(TAG, "save audio cost " + time + " ms");
    }

    long t;

    public void run(ExecutorService executors) {
        t = System.currentTimeMillis();
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
                mPcmDataAlign = new PcmDataAlign();
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
        mDecEncFlag = true;
        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
