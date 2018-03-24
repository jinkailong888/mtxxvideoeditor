package com.meitu.library.videoeditor.media.save;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Surface;

import com.meitu.library.videoeditor.media.MediaEditor;
import com.meitu.library.videoeditor.media.codec.CodecOutputSurface;
import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by wyh3 on 2018/3/22.
 * 保存任务
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SaveTask extends HandlerThread {
    private final static String TAG = Tag.build("SaveTask");

    private final static String VIDEO_ENCODE_MIME = "video/avc";
    private final static String AUDIO_ENCODE_MIME = "audio/mp4a-latm";
    private static final int DECODE_TIMEOUT = 1000;
    private static final int ENCODE_TIMEOUT = 1000;
    private MediaExtractor mVideoMediaExtractor;
    private MediaExtractor mAudioMediaExtractor;
    private MediaExtractor mBgMusicMediaExtractor;
    private int mVideoTrackIndex = -1;
    private int mAudioTrackIndex = -1;
    private int mBgMusicTrackIndex = -1;
    private MediaFormat mVideoTrackFormat;
    private MediaFormat mAudioTrackFormat;
    private MediaCodec mVideoDecoder;
    private MediaCodec mAudioDecoder;
    private MediaCodec mBgMusicDecoder;
    private MediaCodec mVideoEncoder;
    private MediaCodec mAudioEncoder;
    private Surface mEncodeSurface;
    private CodecOutputSurface mCodecOutputSurface;
    private ArrayBlockingQueue<PcmData> mPcmQueue = new ArrayBlockingQueue<>(10);
    private MediaMuxer mMediaMuxer;
    private final Object mMuxerSyncObject = new Object();
    private short mMuxerTrackNum = 0;
    private boolean mMuxerStarted = false;
    private int mAudioMuxerTrackIndex = -1;
    private int mVideoMuxerTrackIndex = -1;
    private Runnable mReadPacketRunnable;
    private Runnable mAudioHandleRunnable;
    private boolean mAudioHandleDone;
    private Runnable mBgMusicHandleRunnable;
    private Runnable mAudioEncodeRunnable;
    private ExecutorService mExecutorService;
    private OnSaveListener mOnSaveListener;
    private VideoSaveInfo mVideoSaveInfo;
    private SaveFilters mSaveFilters;


    private void prepare() throws IOException {
        int proc = Runtime.getRuntime().availableProcessors();
        Log.d(TAG, "prepare: availableProcessors=" + proc);
        mExecutorService = Executors.newCachedThreadPool();
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

//        MediaFormat audioEncodeFormat = MediaFormat.createAudioFormat(AUDIO_ENCODE_MIME,
//                44100, 2);
//        audioEncodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
//        audioEncodeFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
//        byte[] bytes = new byte[]{(byte) 0x11, (byte)0x90};
//        ByteBuffer bb = ByteBuffer.wrap(bytes);
//        audioEncodeFormat.setByteBuffer("csd-0", bb);
//        audioEncodeFormat.setByteBuffer("csd-0", ByteBuffer.allocate(2).put(new byte[]{(byte) 0x11, (byte)0x90}));
//        audioEncodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//        audioEncodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);//作用于inputBuffer的大小
//        mAudioEncoder = MediaCodec.createEncoderByType(AUDIO_ENCODE_MIME);
//        mAudioEncoder.configure(audioEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//        mAudioEncoder.start();

        mVideoMediaExtractor = new MediaExtractor();
//        mAudioMediaExtractor = new MediaExtractor();
        mVideoMediaExtractor.setDataSource(mVideoSaveInfo.getSrcPath());
//        mAudioMediaExtractor.setDataSource(mVideoSaveInfo.getSrcPath());
        for (int i = 0; i < mVideoMediaExtractor.getTrackCount(); i++) {
            MediaFormat format = mVideoMediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            Log.d(TAG, "getTrackFormat index=" + i + " (" + mime + "): " + format);
            if (mime.startsWith("video/")) {
                mVideoTrackIndex = i;
                mVideoTrackFormat = format;
                mCodecOutputSurface = new CodecOutputSurface(
                        mVideoSaveInfo.getOutputWidth(), mVideoSaveInfo.getOutputHeight(),
                        mEncodeSurface, mSaveFilters.isFilter());
                mVideoDecoder = MediaCodec.createDecoderByType(mime);
                mVideoDecoder.configure(format, mCodecOutputSurface.getSurface(), null, 0);
                mVideoDecoder.start();
            }
//            if (mime.startsWith("audio/")) {
//                mAudioTrackIndex = i;
//                mAudioTrackFormat = format;
//                mAudioDecoder = MediaCodec.createDecoderByType(mime);
//                mAudioDecoder.configure(format, null, null, 0);
//                mAudioDecoder.start();
//            }
        }
    }

    private void initReadPacketRunnable() {
        mReadPacketRunnable = new Runnable() {
            @Override
            public void run() {
                int inputBufIndex;
                boolean videoReadDone = false;
                boolean audioReadDone = false;
                ByteBuffer[] videoDecodeInputBuffer = mVideoDecoder.getInputBuffers();
//                ByteBuffer[] audioDecodeInputBuffer = mAudioDecoder.getInputBuffers();
                mVideoMediaExtractor.selectTrack(mVideoTrackIndex);
//                mAudioMediaExtractor.selectTrack(mAudioTrackIndex);
                while (!videoReadDone || !audioReadDone) {
                    if (!videoReadDone) {
                        inputBufIndex = mVideoDecoder.dequeueInputBuffer(DECODE_TIMEOUT);
                        if (inputBufIndex >= 0) {
                            ByteBuffer inputBuf = videoDecodeInputBuffer[inputBufIndex];
                            int sampleSize = mVideoMediaExtractor.readSampleData(inputBuf, 0);
                            if (sampleSize >= 0) {
                                mVideoDecoder.queueInputBuffer(inputBufIndex, 0,
                                        sampleSize, mVideoMediaExtractor.getSampleTime(), 0);
                                boolean eos = !mVideoMediaExtractor.advance();
                                Log.d(TAG, "视频流 queueInputBuffer eos=" + eos);
                            } else {
                                mVideoDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                videoReadDone = true;
                                Log.d(TAG, "视频流已读完");
//                                mVideoMediaExtractor.release();
//                                mVideoMediaExtractor = null;
                            }
                        }
                    }
                    audioReadDone = true;
//                    if (!audioReadDone) {
//                        inputBufIndex = mAudioDecoder.dequeueInputBuffer(DECODE_TIMEOUT);
//                        if (inputBufIndex >= 0) {
//                            ByteBuffer inputBuf = audioDecodeInputBuffer[inputBufIndex];
//                            inputBuf.clear();
//                            int sampleSize = mAudioMediaExtractor.readSampleData(inputBuf, 0);
//                            if (sampleSize > 0) {
//                                mAudioDecoder.queueInputBuffer(inputBufIndex, 0,
//                                        sampleSize, mAudioMediaExtractor.getSampleTime(), 0);
//                                boolean eos = !mAudioMediaExtractor.advance();
//                                Log.d(TAG, "音频流 queueInputBuffer eos=" + eos);
//                            } else {
//                                mAudioDecoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
//                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                                audioReadDone = true;
//                                Log.d(TAG, "音频流已读完");
//                                mAudioMediaExtractor.release();
//                                mAudioMediaExtractor = null;
//                            }
//                        } else {
//                            Log.d(TAG, "音频解码器没有可用的 inputBuffer");
//                        }
//                    }
                }
            }
        };
        mExecutorService.execute(mReadPacketRunnable);
    }


    private void initVideo() {
        boolean decodeDone = false;
        int decodeCount = 0;
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
                    // 渲染必须在mCodecOutputSurface初始化的线程
                    mCodecOutputSurface.awaitNewImage();
                    mCodecOutputSurface.drawImage(true);
                    writeVideoData(false);
                    mCodecOutputSurface.setPresentationTime(computePresentationTimeNsec(decodeCount));
                    mCodecOutputSurface.swapBuffers();
                    decodeCount++;
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "视频帧解码完毕");
                    decodeDone = true;
                    writeVideoData(true);
                    mVideoDecoder.stop();
                    mVideoDecoder.release();
                    mVideoDecoder = null;
                    mCodecOutputSurface.release();
                    mCodecOutputSurface = null;
                }
            }
        }
    }


    private void writeVideoData(boolean endOfStream) {
        if (endOfStream) {
            mVideoEncoder.signalEndOfInputStream();
        }
        ByteBuffer[] encoderOutputBuffers = mVideoEncoder.getOutputBuffers();
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        while (true) {
            int encoderStatus = mVideoEncoder.dequeueOutputBuffer(bufferInfo, ENCODE_TIMEOUT);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (endOfStream) {
                    //如果读取结束
                } else {
                    break;
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = mVideoEncoder.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                mVideoMuxerTrackIndex = addVideoMuxerTrack(mVideoEncoder.getOutputFormat());
            } else if (encoderStatus < 0) {
            } else {
                ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (bufferInfo.size > 0 && mMuxerStarted) {
                    Log.d(TAG, "写入视频数据");
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);
                    writeSampleData(mVideoMuxerTrackIndex, encodedData, bufferInfo);
                }
                mVideoEncoder.releaseOutputBuffer(encoderStatus, false);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "写完视频数据");
                    mVideoEncoder.stop();
                    mVideoEncoder.release();
                    mVideoEncoder = null;
                    writeDone();
                    break;
                }
            }
        }
    }

    private void initAudioHandleRunnable() {
        mAudioHandleRunnable = new Runnable() {
            @Override
            public void run() {
                boolean decodeDone = false;
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                ByteBuffer[] decodeOutputBuffers = mAudioDecoder.getOutputBuffers();
                while (!decodeDone) {
                    int decoderStatus = mAudioDecoder.dequeueOutputBuffer(bufferInfo, DECODE_TIMEOUT);
                    if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        Log.d(TAG, "没有解码后的音频帧");
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        decodeOutputBuffers = mAudioDecoder.getOutputBuffers();
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                    } else if (decoderStatus < 0) {
                        Log.d(TAG, "initAudio decoderStatus < 0");
                    } else {
                        if (bufferInfo.size > 0) {
                            Log.d(TAG, "音频帧解码完一帧");
                            ByteBuffer byteBuffer = decodeOutputBuffers[decoderStatus];
                            byte[] data = new byte[bufferInfo.size];
                            byteBuffer.get(data);
                            byteBuffer.clear();
                            putPcmData(new PcmData(data, bufferInfo.presentationTimeUs));
                        }
                        mAudioDecoder.releaseOutputBuffer(decoderStatus, false);
                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.d(TAG, "音频帧解码完毕");
                            mAudioDecoder.stop();
                            mAudioDecoder.release();
                            mAudioDecoder = null;
                            decodeDone = true;
                            //todo 暂时跳过处理步骤
                            mAudioHandleDone = true;

                        }
                    }
                }


            }
        };
        mExecutorService.execute(mAudioHandleRunnable);
    }

    private void initAudioEncodeRunnable() {
        mAudioEncodeRunnable = new Runnable() {
            @Override
            public void run() {
                boolean encodeReadDone = false;
                ByteBuffer[] encodeInputBuffers = mAudioEncoder.getInputBuffers();
                ByteBuffer[] encodeOutputBuffers = mAudioEncoder.getOutputBuffers();
                while (true) {
                    if (!encodeReadDone) {
                        int index = mAudioEncoder.dequeueInputBuffer(ENCODE_TIMEOUT);
                        if (index > 0) {
                            PcmData pcmData = pollPcmData();
                            ByteBuffer inputBuffer = encodeInputBuffers[index];
                            inputBuffer.clear();
                            if (pcmData == null) {
                                if (mAudioHandleDone) {
                                    Log.d(TAG, "读取完pcm了");
                                    mAudioEncoder.queueInputBuffer(index, 0, 0,
                                            0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                    encodeReadDone = true;
                                } else {
                                    Log.d(TAG, "编码线程未从缓冲队列中读到帧数据");
                                }
                            } else {
                                inputBuffer.limit(pcmData.data.length);
                                inputBuffer.put(pcmData.data);
                                Log.d(TAG, "把pcm数据加入编码队列");
                                mAudioEncoder.queueInputBuffer(index, 0, pcmData.data.length,
                                        pcmData.pts, 0);
                            }
                        }
                    }
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int encodeStatus = mAudioEncoder.dequeueOutputBuffer(bufferInfo, ENCODE_TIMEOUT);
                    if (encodeStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    } else if (encodeStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        encodeOutputBuffers = mAudioEncoder.getOutputBuffers();
                    } else if (encodeStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        mAudioMuxerTrackIndex = addAudioMuxerTrack(mAudioTrackFormat);
                    } else if (encodeStatus < 0) {
                    } else {
                        ByteBuffer encodedData = encodeOutputBuffers[encodeStatus];
                        if (bufferInfo.size > 0 && mMuxerStarted) {
                            Log.d(TAG, "写入音频数据");
                            encodedData.position(bufferInfo.offset);
                            encodedData.limit(bufferInfo.offset + bufferInfo.size);

                            bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;

//                            writeSampleData(mAudioMuxerTrackIndex, encodedData, bufferInfo);
                        }
                        mAudioEncoder.releaseOutputBuffer(encodeStatus, false);
                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.d(TAG, "写完音频数据");
                            writeDone();
                            mAudioEncoder.stop();
                            mAudioEncoder.release();
                            mAudioEncoder = null;
                            break;
                        }
                    }
                }
            }
        };
        mExecutorService.execute(mAudioEncodeRunnable);
    }

    private synchronized void writeDone() {
        mMuxerTrackNum--;
        if (mMuxerTrackNum == 0) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mMediaMuxer = null;
        }
    }

    private synchronized void writeSampleData(int trackIndex, @NonNull ByteBuffer byteBuf,
                                              @NonNull MediaCodec.BufferInfo bufferInfo) {
        mMediaMuxer.writeSampleData(trackIndex, byteBuf, bufferInfo);
    }


    private void putPcmData(PcmData pcmData) {
        try {
            mPcmQueue.put(pcmData);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private PcmData pollPcmData() {
        PcmData data = null;
        try {
            data = mPcmQueue.poll(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return data;
    }

    private void handleAudioData(boolean b) {
    }

    //音频和视频轨道都添加后才能开始
    private int addVideoMuxerTrack(MediaFormat outputFormat) {
        synchronized (mMuxerSyncObject) {
            Log.d(TAG, "addMuxerTrack Video");
            int index = mMediaMuxer.addTrack(outputFormat);
            mMuxerTrackNum++;
            mMediaMuxer.start();

//            if (mMuxerTrackNum == 1) { //音频在等
//                Log.d(TAG, "唤醒音频");
//                mMuxerSyncObject.notifyAll();
//            }
            return index;
        }
    }

    private int addAudioMuxerTrack(MediaFormat outputFormat) {
        synchronized (mMuxerSyncObject) {
            Log.d(TAG, "addMuxerTrack Audio");
            while (mMuxerTrackNum == 0) {
                try {
                    mMuxerSyncObject.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int index = mMediaMuxer.addTrack(outputFormat);
            mMuxerTrackNum++;
            if (mMuxerTrackNum == 2) {
                mMediaMuxer.start();
                mMuxerStarted = true;
            }
            return index;
        }
    }

    private static long computePresentationTimeNsec(int frameIndex) {
        return frameIndex * 1000000000L / 30;
    }


    private SaveTask(VideoSaveInfo videoSaveInfo, SaveFilters saveFilters, String name) {
        super(name);
        mVideoSaveInfo = videoSaveInfo;
        mSaveFilters = saveFilters;
    }

    public static void save(VideoSaveInfo v, SaveFilters saveFilters) {
        new SaveTask(v, saveFilters, "surface-thread").start();
    }

    @Override
    public void run() {
        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initReadPacketRunnable();
//        initAudioHandleRunnable();
//        initAudioEncodeRunnable();
        initVideo();
    }


    private boolean mixBgMusic() {
        return mSaveFilters != null && mSaveFilters.getBgMusicInfo() != null;
    }


}
