package com.meitu.library.videoeditor.media;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.meitu.library.videoeditor.media.codec.EncodeDecodeSurface;
import com.meitu.library.videoeditor.media.save.SaveFilters;
import com.meitu.library.videoeditor.media.save.SaveTask;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * Created by wyh3 on 2018/3/14.
 * 硬件转码
 */

public class MediaEditor extends Thread {
    private final static String TAG = Tag.build("MediaEditor");

    private String mSrcPath;

    private MediaExtractor mMediaExtractor;
    private MediaCodec mMediaDeCodec;
    private MediaCodec mMediaEncodec;
    private MediaMuxer mMediaMuxer;
    public int mMuxerTrackIndex;

    private VideoSaveInfo mVideoSaveInfo;
    private boolean mDecodeReadDone = false;
    private boolean mDecodeDone = false;
    private boolean mEncodeReadDone = false;

    public static ArrayBlockingQueue<YuvData> YUVQueue = new ArrayBlockingQueue<>(10);
    private boolean mMediaMuxerStart;

    private String VIDEO_MIME_TYPE = "video/avc";

//    private String AUDIO_MIME_TYPE = "audio";


    class YuvData {
        byte[] data;
        long pts;

        public YuvData(byte[] data, long pts) {
            this.data = data;
            this.pts = pts;
        }
    }

    private int mWidth, mHeight;

    ByteBuffer[] mDecInputBuffers;
    ByteBuffer[] mDecOutputBuffers;
    ByteBuffer[] mEncInputBuffers;
    ByteBuffer[] mEncOutputBuffers;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    MediaEditor(String srcPath, VideoSaveInfo v) {
        mSrcPath = srcPath;
        mVideoSaveInfo = v;
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaMuxer = new MediaMuxer(v.getVideoSavePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            mMediaExtractor.setDataSource(mSrcPath);
        } catch (IOException e) {
            e.printStackTrace();
        }


        int trackIndex = getVideoTrack(mMediaExtractor);
        mMediaExtractor.selectTrack(trackIndex);

        MediaFormat format = mMediaExtractor.getTrackFormat(trackIndex);
        Log.d(TAG, "decode width: " + format.getInteger(MediaFormat.KEY_WIDTH));
        Log.d(TAG, "decode height: " + format.getInteger(MediaFormat.KEY_HEIGHT));
        Log.d(TAG, "decode mimeType: " + format.getString(MediaFormat.KEY_MIME));
        String mimeType = format.getString(MediaFormat.KEY_MIME);

        mWidth = format.getInteger(MediaFormat.KEY_WIDTH);
        mHeight = format.getInteger(MediaFormat.KEY_HEIGHT);


        try {
            mMediaDeCodec = MediaCodec.createDecoderByType(mimeType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaDeCodec.configure(format, null, null, 0);
        mMediaDeCodec.start();

        mDecInputBuffers = mMediaDeCodec.getInputBuffers();
        mDecOutputBuffers = mMediaDeCodec.getOutputBuffers();

        initEncoder(mimeType);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run() {
        while (true) {
            if (!mDecodeReadDone) {
                int index = mMediaDeCodec.dequeueInputBuffer(9000);
                if (index >= 0) {
                    ByteBuffer inputBuffer = mDecInputBuffers[index];
                    if (inputBuffer != null) {
                        inputBuffer.clear();
                        int sampleSize = mMediaExtractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            mMediaDeCodec.queueInputBuffer(index, 0, 0,
                                    0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            mDecodeReadDone = true;
                        } else {
                            mMediaDeCodec.queueInputBuffer(index, 0, sampleSize,
                                    mMediaExtractor.getSampleTime(), 0);
                        }
                        mMediaExtractor.advance();
                    }
                }
            }

            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
            int index = mMediaDeCodec.dequeueOutputBuffer(mBufferInfo, 9000);
            switch (index) {
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    MediaFormat newFormat = mMediaDeCodec.getOutputFormat();
                    Log.d(TAG, "decoder output format changed: " + newFormat);
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.d(TAG, "解码当前帧超时");
                    break;
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    Log.d(TAG, "output buffers changed");
                    mDecOutputBuffers = mMediaDeCodec.getOutputBuffers();
                    break;
                default:
                    //渲染
                    Log.d(TAG, "解码得到帧");
                    ByteBuffer outputBuffer = mDecOutputBuffers[index];
                    if (outputBuffer != null) {
                        byte[] data = new byte[mBufferInfo.size];
                        outputBuffer.get(data);
                        outputBuffer.clear();
                        putData(data, mBufferInfo.presentationTimeUs);
                    }
                    mMediaDeCodec.releaseOutputBuffer(index, false);
                    break;
            }
            // All decoded frames have been rendered, we can stop playing now
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "所有帧解码完毕");
                mDecodeDone = true;
                mMediaDeCodec.stop();
                mMediaDeCodec.release();
                mMediaDeCodec = null;
                break;
            }
        }
    }


    private YuvData getData() {
        YuvData data = null;
        try {
            data = YUVQueue.poll(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return data;
    }

    private void putData(byte[] data, long pts) {
        try {
            YUVQueue.put(new YuvData(data, pts));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void initEncoder(String mimeType) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            try {
                mMediaEncodec = MediaCodec.createEncoderByType(VIDEO_MIME_TYPE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(VIDEO_MIME_TYPE, mWidth, mHeight);
            if (mimeType.startsWith("video")) {
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2000001);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            }
            if (mimeType.startsWith("audio")) {
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mHeight * mWidth * 15);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 30);
            }
            mMediaEncodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaEncodec.start();
            mEncInputBuffers = mMediaEncodec.getInputBuffers();
            mEncOutputBuffers = mMediaEncodec.getOutputBuffers();
            new Thread(new EncodeRunnable()).start();
        }
    }


    private class EncodeRunnable implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            while (true) {
                if (!mEncodeReadDone) {
                    int index = mMediaEncodec.dequeueInputBuffer(9000);
                    if (index > 0) {
                        ByteBuffer inputBuffer = mMediaEncodec.getInputBuffer(index);
                        if (inputBuffer != null) {
                            YuvData data = getData();
                            if (data == null) {
                                if (mDecodeDone) {
                                    Log.d(TAG, "读取完帧了");
                                    mMediaEncodec.queueInputBuffer(index, 0, 0,
                                            0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                    mEncodeReadDone = true;
                                } else {
                                    Log.d(TAG, "编码线程未从缓冲队列中读到帧数据");
                                }
                            } else {
                                inputBuffer.clear();
                                inputBuffer.put(data.data);
                                Log.d(TAG, "把帧数据加入编码队列");
                                mMediaEncodec.queueInputBuffer(index, 0, data.data.length,
                                        data.pts, 0);
                            }
                        }
                    }
                }

                MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
                int index = mMediaEncodec.dequeueOutputBuffer(mBufferInfo, 9000);
                switch (index) {
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        MediaFormat newFormat = mMediaEncodec.getOutputFormat();
                        Log.d(TAG, "encoder output format changed: " + newFormat);
                        mMuxerTrackIndex = mMediaMuxer.addTrack(newFormat);
                        mMediaMuxer.start();
                        mMediaMuxerStart = true;
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d(TAG, "编码当前帧超时");
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        mEncOutputBuffers = mMediaEncodec.getOutputBuffers();
                        Log.d(TAG, "encode output buffers changed");
                        break;
                    default:
                        Log.d(TAG, "编码得到压缩数据");
                        ByteBuffer byteBuffer = mEncOutputBuffers[index];
                        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            mBufferInfo.size = 0;
                        }
                        if (mBufferInfo.size != 0) {
                            if (!mMediaMuxerStart) {
                                throw new RuntimeException("muxer hasn't started");
                            }
                            byteBuffer.position(mBufferInfo.offset);
                            byteBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
                            mMediaMuxer.writeSampleData(mMuxerTrackIndex, byteBuffer, mBufferInfo);
                        }
                        mMediaEncodec.releaseOutputBuffer(index, false);
                        break;
                }
                // All decoded frames have been rendered, we can stop playing now
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "所有帧编码完毕");


                    mMediaEncodec.stop();
                    mMediaEncodec.release();
                    mMediaEncodec = null;

                    mMediaMuxer.stop();
                    mMediaMuxer.release();
                    mMediaMuxer = null;


                    Log.d(TAG, "MediaEditor save video 耗时 : " + (
                            System.currentTimeMillis() - MediaEditor.startTime));


                    break;
                }
            }
        }
    }

    public static long startTime;


    public static void save(VideoSaveInfo v, boolean filter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "SupportAvcCodec=" + SupportAvcCodec());

            startTime = System.currentTimeMillis();

//            new MediaEditor(v.getSrcPath(), v).start();// 11232  11024
            if (!filter) {
                SaveFilters saveFilters = new SaveFilters();
                saveFilters.setFilter(filter);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    SaveTask.save(v, saveFilters);
                }
            } else {
                EncodeDecodeSurface test = new EncodeDecodeSurface(v,filter); //10953 10775
                test.testEncodeDecodeSurface();
            }





        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private int getVideoTrack(MediaExtractor extractor) {
        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                return i;
            }
        }
        return -1;
    }


    private static boolean SupportAvcCodec() {
        if (Build.VERSION.SDK_INT >= 18) {
            for (int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--) {
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);
                String[] types = codecInfo.getSupportedTypes();
                for (String type : types) {
                    if (type.equalsIgnoreCase("video/avc")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


}
