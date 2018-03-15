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
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;


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
    private VideoSaveInfo mVideoSaveInfo;
    private boolean mDecodeReadDone = false;
    private boolean mDecodeDone = false;
    private boolean mEncodeReadDone = false;
    private ArrayList<byte[]> mChunkDataList = new ArrayList<>();


    private int mWidth, mHeight;


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

        int numTracks = mMediaExtractor.getTrackCount();
        int videoIndex = 0, audioIndex = 0;

        for (int i = 0; i < numTracks; i++) {
            MediaFormat trackFormat = mMediaExtractor.getTrackFormat(i);
            String mimeType = trackFormat.getString(MediaFormat.KEY_MIME);
            // 取出视频的信号
            if (mimeType.startsWith("video/")) {
                videoIndex = i;
            } else if (mimeType.startsWith("audio/")) {
                audioIndex = i;
            }
        }

        int trackIndex = videoIndex;

        mMediaExtractor.selectTrack(trackIndex);
        MediaFormat format = mMediaExtractor.getTrackFormat(trackIndex);
//        Log.d(TAG, "decode format: " + format.getInteger(MediaFormat.KEY_COLOR_FORMAT));
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
        initEncoder(mimeType);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void run() {
        while (true) {
            if (!mDecodeReadDone) {
                int index = mMediaDeCodec.dequeueInputBuffer(9000);
                if (index > 0) {
                    ByteBuffer inputBuffer = mMediaDeCodec.getInputBuffer(index);
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
                    Log.d(TAG, "format changed");
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    Log.d(TAG, "解码当前帧超时");
                    break;
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    //outputBuffers = videoCodec.getOutputBuffers();
                    Log.d(TAG, "output buffers changed");
                    break;
                default:
                    //渲染
                    Log.d(TAG, "run: 解码得到帧");
                    ByteBuffer outputBuffer = mMediaDeCodec.getOutputBuffer(index);
                    if (outputBuffer != null) {
                        byte[] data = new byte[mBufferInfo.size];
                        outputBuffer.get(data);
                        outputBuffer.clear();
                        if (mChunkDataList.size() < 20) {
                            putData(data);
                        }

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
                break;
            }
        }
    }


    private byte[] getData() {
        synchronized (MediaEditor.class) {//记得加锁
            if (mChunkDataList.isEmpty()) {
                Log.d(TAG, "getData mChunkDataList.isEmpty()");
                return null;
            }
            byte[] data = mChunkDataList.get(0);//每次取出index 0 的数据
            mChunkDataList.remove(data);//取出后将此数据remove掉 既能保证PCM数据块的取出顺序 又能及时释放内存
            Log.d(TAG, "getData data==null?" + (data == null));
            return data;
        }
    }

    private void putData(byte[] data) {
        synchronized (MediaEditor.class) {//记得加锁
            Log.d(TAG, "putData data==null?" + (data == null));
            mChunkDataList.add(data);
        }
    }


    private void initEncoder(String mimeType) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            try {
                mMediaEncodec = MediaCodec.createEncoderByType(mimeType);
            } catch (IOException e) {
                e.printStackTrace();
            }
            MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, mWidth, mHeight);
            if (mimeType.startsWith("video")) {
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 2000001);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            }
            if (mimeType.startsWith("audio")) {
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mHeight * mWidth * 15);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 60);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 30);
            }
            mMediaEncodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaEncodec.start();
            new Thread(new EncodeRunnable()).start();
        }
    }


    private class EncodeRunnable implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void run() {
            while (true) {
                if (!mEncodeReadDone) {
                    if (mChunkDataList.isEmpty()) {
                        Log.d(TAG, "mChunkDataList 为空， 不 申请  inputbuffer");
                    } else {
                        int index = mMediaEncodec.dequeueInputBuffer(9000);
                        if (index > 0) {
                            ByteBuffer inputBuffer = mMediaEncodec.getInputBuffer(index);
                            if (inputBuffer != null) {
                                byte[] data = getData();
                                if (data == null) {
                                    if (mDecodeDone) {
                                        Log.d(TAG, "读取完帧了");
                                        mMediaEncodec.queueInputBuffer(index, 0, 0,
                                                0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                        mEncodeReadDone = true;
                                    } else {
                                        Log.d(TAG, "获取了一个inputbuffer，但是没用");
                                    }
                                } else {
                                    inputBuffer.clear();
                                    inputBuffer.put(data);
                                    Log.d(TAG, "把帧数据加入编码队列");
                                    mMediaEncodec.queueInputBuffer(index, 0, data.length,
                                            0, 0);
                                }
                            }
                        } else {
                            Log.e(TAG, "mMediaEncodec.dequeueInputBuffer 《 0  ");
                        }
                    }
                }

                MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
                int index = mMediaEncodec.dequeueOutputBuffer(mBufferInfo, 9000);
                switch (index) {
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        Log.d(TAG, "encode format changed");
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        Log.d(TAG, "编码当前帧超时");
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        //outputBuffers = videoCodec.getOutputBuffers();
                        Log.d(TAG, "encode output buffers changed");
                        break;
                    default:
                        //渲染
                        Log.d(TAG, "编码得到压缩数据");

                        mMediaEncodec.releaseOutputBuffer(index, false);
                        break;
                }
                // All decoded frames have been rendered, we can stop playing now
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "所有帧编码完毕");
                    break;
                }
            }
        }
    }

    public static long startTime;


    public static void save(VideoSaveInfo v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.d(TAG, "SupportAvcCodec=" + SupportAvcCodec());


//            new MediaEditor(srcPath, v).start();
            startTime = System.currentTimeMillis();

            EncodeDecodeSurface test = new EncodeDecodeSurface(v);
            try {
                test.testEncodeDecodeSurface();
            } catch (Throwable a) {
                a.printStackTrace();
            }


        }
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
