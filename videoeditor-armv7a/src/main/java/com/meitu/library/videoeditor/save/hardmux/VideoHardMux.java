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
import com.meitu.library.videoeditor.save.video.ColorFormatUtil;
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
public class VideoHardMux implements Runnable {
    private final static String TAG = Tag.build("VideoHardMux");

    private final static String ENCODE_MIME = "video/avc";
    private static final int ENCODE_TIMEOUT = 1000;
    private static final int notifyAudioCount = 1; //写入一定数量视频帧后再开始写音频帧
    private int mWriteCount = 0;
    private final Object VideoWroteLock;
    public static boolean videoWrote = false;
    private MediaCodec mEncoder;
    private volatile boolean mDecodeDone;
    private MuxStore mMuxStore;
    private int mMuxerTrackIndex = -1;
    private ArrayBlockingQueue<AVData> mVideoRgbQueue = new ArrayBlockingQueue<>(5);
    private VideoSaveInfo mVideoSaveInfo;
    private SaveFilters mSaveFilters;


    private void encode() {
        boolean readDone = false;
        ByteBuffer[] encodeInputBuffers = mEncoder.getInputBuffers();
        ByteBuffer[] encodeOutputBuffers = mEncoder.getOutputBuffers();
        while (true) {
            if (!readDone) {
                int bufIndex = mEncoder.dequeueInputBuffer(ENCODE_TIMEOUT);
                if (bufIndex > 0) {
                    AVData rgbData = AVDataUtil.pollAVData(mVideoRgbQueue);
                    ByteBuffer inputBuffer = encodeInputBuffers[bufIndex];
                    inputBuffer.clear();
                    if (rgbData == null) {
                        if (mDecodeDone) {
                            Log.d(TAG, "encode 读取完rgb了");
                            mEncoder.queueInputBuffer(bufIndex, 0, 0,
                                    0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            readDone = true;
                        } else {
                            Log.d(TAG, "encode 编码线程未从缓冲队列中读到帧数据");
                        }
                    } else {
                        inputBuffer.limit(rgbData.data.length);
                        inputBuffer.put(rgbData.data);
                        Log.d(TAG, "encode 把rgb数据加入编码队列");
                        mEncoder.queueInputBuffer(bufIndex, 0, rgbData.data.length,
                                rgbData.pts, 0);
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
            } else if (encodeStatus < 0) {
            } else {
                ByteBuffer byteBuffer = encodeOutputBuffers[encodeStatus];
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "encode 写完视频数据");
                    if (!videoWrote) {
                        synchronized (VideoWroteLock) {
                            videoWrote = true;
                            VideoWroteLock.notifyAll();
                        }
                    }
                    break;
                }
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    bufferInfo.size = 0;
                }
                if (bufferInfo.size > 0) {
                    Log.d(TAG, "encode 写入视频数据");
                    byteBuffer.position(bufferInfo.offset);
                    byteBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    Log.d(TAG, VELog.toString(bufferInfo));
                    mMuxStore.addData(mMuxerTrackIndex, byteBuffer, bufferInfo);
                    if (!videoWrote) {
                        mWriteCount++;
                        if (mWriteCount == notifyAudioCount) {
                            synchronized (VideoWroteLock) {
                                videoWrote = true;
                                VideoWroteLock.notifyAll();
                                Log.d(TAG, "encode: 通知音频线程解锁");
                            }
                        }
                    }
                }
                mEncoder.releaseOutputBuffer(encodeStatus, false);
            }
        }


    }

    public void encode(byte[] data, long pts) {
        Log.d(TAG, "encode: data.length=" + data.length + " pts=" + pts);
        AVDataUtil.putAVData(mVideoRgbQueue, new AVData(data, pts));
    }

    private void release() {
        mEncoder.stop();
        mEncoder.release();
        mEncoder = null;
    }


    private void prepare() throws IOException {
        MediaFormat videoEncodeFormat = MediaFormat.createVideoFormat(ENCODE_MIME,
                mVideoSaveInfo.getOutputWidth(), mVideoSaveInfo.getOutputHeight());

        videoEncodeFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,ColorFormatUtil.selectColorFormat(ENCODE_MIME));
        videoEncodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, mVideoSaveInfo.getOutputBitrate());
        videoEncodeFormat.setInteger(MediaFormat.KEY_FRAME_RATE, mVideoSaveInfo.getFps());
        videoEncodeFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mVideoSaveInfo.getIFrameInterval());
        mEncoder = MediaCodec.createEncoderByType(ENCODE_MIME);
        mEncoder.configure(videoEncodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mEncoder.start();
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


    public VideoHardMux(VideoSaveInfo videoSaveInfo, SaveFilters saveFilters, MuxStore muxStore, Object videoWroteLock) {
        VideoWroteLock = videoWroteLock;
        mVideoSaveInfo = videoSaveInfo;
        mSaveFilters = saveFilters;
        mMuxStore = muxStore;
    }

    public void decodeDone() {
        mDecodeDone = true;
    }

    public void run(ExecutorService executors) {
        executors.execute(this);
    }
}
