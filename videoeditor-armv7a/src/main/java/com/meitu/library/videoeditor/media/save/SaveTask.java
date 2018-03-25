package com.meitu.library.videoeditor.media.save;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.meitu.library.videoeditor.media.save.audio.AudioConverter;
import com.meitu.library.videoeditor.media.save.muxer.Mp4MuxStore;
import com.meitu.library.videoeditor.media.save.muxer.MuxStore;
import com.meitu.library.videoeditor.media.save.video.VideoConverter;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by wyh3 on 2018/3/22.
 * 保存任务
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SaveTask extends Thread {
    private final static String TAG = Tag.build("SaveTask");

    private VideoSaveInfo mVideoSaveInfo;
    private SaveFilters mSaveFilters;
    private VideoConverter mVideoConverter;
    private AudioConverter mAudioConverter;
    private ExecutorService mExecutors;
    private MuxStore mMuxStore;


    private static final boolean ignoreAudio = false;

    private void prepare() throws IOException {
        mExecutors = Executors.newCachedThreadPool();
        mMuxStore = new Mp4MuxStore(mVideoSaveInfo.getVideoSavePath(), mVideoSaveInfo.getRotate());
        mMuxStore.setIgnoreAudio(ignoreAudio);
        mVideoConverter = new VideoConverter(mVideoSaveInfo, mSaveFilters, mMuxStore);
        if (!ignoreAudio) {
            mAudioConverter = new AudioConverter(mVideoSaveInfo, mSaveFilters, mMuxStore);
        }
    }

    @Override
    public void run() {
        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mMuxStore.close();
    }

    private SaveTask(VideoSaveInfo videoSaveInfo, SaveFilters saveFilters, String name) {
        super(name);
        mVideoSaveInfo = videoSaveInfo;
        mSaveFilters = saveFilters;
    }

    public static void save(VideoSaveInfo v, SaveFilters saveFilters) {
        new SaveTask(v, saveFilters, "surface-thread").start();
    }


}
