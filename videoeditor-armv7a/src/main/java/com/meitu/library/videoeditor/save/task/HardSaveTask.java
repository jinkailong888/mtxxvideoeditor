package com.meitu.library.videoeditor.save.task;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.save.bean.SaveFilters;
import com.meitu.library.videoeditor.save.audio.AudioConverter;
import com.meitu.library.videoeditor.save.muxer.Mp4MuxStore;
import com.meitu.library.videoeditor.save.muxer.MuxStore;
import com.meitu.library.videoeditor.save.video.VideoConverter;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wyh3 on 2018/3/22.
 * 硬解硬保
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class HardSaveTask extends ISaveTask {
    private final static String TAG = Tag.build("HardSaveTask");

    private VideoSaveInfo mVideoSaveInfo;
    private SaveFilters mSaveFilters;
    private VideoConverter mVideoConverter;
    private AudioConverter mAudioConverter;
    private ExecutorService mExecutors;
    private MuxStore mMuxStore;
    private Object VideoWroteLock = new Object();

    private static final boolean ignoreAudio = false;

    private OnSaveListener mOnSaveListener; //以视频进度为准

    @Override
    public void prepare() {
        mExecutors = Executors.newCachedThreadPool();
        mMuxStore = new Mp4MuxStore(mVideoSaveInfo.getVideoSavePath(), mVideoSaveInfo.getRotate());
        mMuxStore.setIgnoreAudio(ignoreAudio);
        mVideoConverter = new VideoConverter(mVideoSaveInfo, mSaveFilters, mMuxStore, VideoWroteLock,mOnSaveListener);
        if (!ignoreAudio) {
            mAudioConverter = new AudioConverter(mVideoSaveInfo, mSaveFilters, mMuxStore, VideoWroteLock);
        }
    }

    @Override
    public void run() {
        long t = System.currentTimeMillis();
        prepare();
        mVideoConverter.run(mExecutors);
        if (!ignoreAudio) {
            mAudioConverter.run(mExecutors);
        }
        mExecutors.shutdown();
        while (true) {
            if (mExecutors.isTerminated()) {
                Log.d(TAG, "isTerminated ");
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mMuxStore.close();
        if (mOnSaveListener != null) {
            mOnSaveListener.onDone();
        }
        long time = System.currentTimeMillis() - t;
        Log.d(TAG, "save  cost " + time + " ms");
    }

    public HardSaveTask(VideoSaveInfo videoSaveInfo, SaveFilters saveFilters, OnSaveListener onSaveListener) {
        mVideoSaveInfo = videoSaveInfo;
        mSaveFilters = saveFilters;
        mOnSaveListener = onSaveListener;
        if (mOnSaveListener != null) {
            mOnSaveListener.onStart();
        }
    }

}
