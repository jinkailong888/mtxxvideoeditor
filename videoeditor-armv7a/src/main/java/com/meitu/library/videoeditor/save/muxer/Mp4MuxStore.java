package com.meitu.library.videoeditor.save.muxer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.util.Log;

import com.meitu.library.videoeditor.util.Tag;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Mp4MuxStore implements MuxStore {

    private static final String TAG = Tag.build("Mp4MuxStore");
    private MediaMuxer mMuxer;
    private String outputPath;
    private int rotate;
    private int audioTrack = -1;
    private int videoTrack = -1;
    private final Object Lock = new Object();
    private boolean muxStarted = false;
    private LinkedBlockingQueue<MuxData> cache;
    private Recycler<MuxData> recycler;
    private ExecutorService exec;

    private boolean mIgnoreAudio = false;

    public Mp4MuxStore(String outputPath, int rotate) {
        this.outputPath = outputPath;
        this.rotate = rotate;
        cache = new LinkedBlockingQueue<>(30);
        recycler = new Recycler<>();
        exec = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES,
                new LinkedBlockingQueue<Runnable>(16), Executors.defaultThreadFactory());
    }

    public void setIgnoreAudio(boolean ignore) {
        this.mIgnoreAudio = ignore;
    }


    @Override
    public void close() {
        Log.d(TAG, "close 1");
        synchronized (Lock) {
            Log.d(TAG, "close 2");
            if (muxStarted) {
                audioTrack = -1;
                videoTrack = -1;
                muxStarted = false;
            }
        }
    }


    private void muxRun() {
        Log.d(TAG, "muxRun");
        while (muxStarted) {
            try {
                MuxData data = cache.poll(2, TimeUnit.SECONDS);
                synchronized (Lock) {
                    Log.d(TAG, "data is null?" + (data == null));
                    if (data == null) {
                        //TODO 有风险，最好的结束逻辑为主动调用close，而不是判断数据为空
//                        audioTrack = -1;
//                        videoTrack = -1;
//                        muxStarted = false;
                        continue;
                    }
                    if (muxStarted) {
                        Log.d(TAG, "muxRun: writeSampleData  data.index = " + data.index);
                        mMuxer.writeSampleData(data.index, data.data, data.info);
                        recycler.put(data.index, data);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            mMuxer.stop();
            Log.d(TAG, "muxer stoped success");
            mMuxer.release();
        } catch (IllegalStateException e) {
            Log.e(TAG, e.toString());
        }
        mMuxer = null;
        cache.clear();
        recycler.clear();
    }


    @Override
    public int addTrack(MediaFormat mediaFormat) {
        if (mIgnoreAudio) {
            return addVideoTrack(mediaFormat);
        }
        Log.d(TAG, "addTrack->");
        int ret = -1;
        synchronized (Lock) {
            if (!muxStarted) {
                if (audioTrack == -1 && videoTrack == -1) {
                    try {
                        mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "create MediaMuxer failed:" + e.getMessage());
                    }
                    if (rotate != 0) {
                        mMuxer.setOrientationHint(rotate);
                    }
                }
                String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {
                    audioTrack = mMuxer.addTrack(mediaFormat);
                    ret = audioTrack;
                } else if (mime.startsWith("video")) {
                    videoTrack = mMuxer.addTrack(mediaFormat);
                    ret = videoTrack;
                }
                startMux();
            }
        }
        return ret;
    }

    private int addVideoTrack(MediaFormat mediaFormat) {
        int ret = -1;
        if (!muxStarted) {
            if (videoTrack == -1) {
                try {
                    mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "create MediaMuxer failed:" + e.toString() + e.getLocalizedMessage() + e.getCause());
                }
                mMuxer.setOrientationHint(rotate);
            }
            videoTrack = mMuxer.addTrack(mediaFormat);
            ret = videoTrack;
            startMux();
        }
        return ret;
    }

    private void startMux() {
        boolean canMux = audioTrack != -1 && videoTrack != -1;
        canMux = mIgnoreAudio ? videoTrack != -1 : canMux;
        if (canMux) {
            mMuxer.start();
            muxStarted = true;
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    muxRun();
                }
            });
        }
    }

    @Override
    public int addData(int track, ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        if (track >= 0) {
            Log.d(TAG, "addData->" + track + "/audio=" + audioTrack + "/video=" + videoTrack);
            MuxData muxData = new MuxData(byteBuffer, bufferInfo);
            muxData.index = track;
            if (track == audioTrack || track == videoTrack) {
                MuxData d = recycler.poll(track);
                if (d == null) {
                    d = muxData.copy();
                } else {
                    muxData.copyTo(d);
                }
                while (!cache.offer(d)) {
                    Log.d(TAG, "put data to the cache : poll");
                    MuxData c = cache.poll();
                    recycler.put(c.index, c);
                }
            }
        }
        return 0;
    }

    @Override
    public MediaMuxer get() {
        return mMuxer;
    }
}
