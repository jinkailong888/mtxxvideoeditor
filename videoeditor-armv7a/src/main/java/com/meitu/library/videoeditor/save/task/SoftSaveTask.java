package com.meitu.library.videoeditor.save.task;

import android.util.Log;

import com.meitu.library.videoeditor.save.bean.SaveFilters;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by wyh3 on 2018/3/26.
 * 软解软保
 */

public class SoftSaveTask extends ISaveTask {
    private static final String TAG = Tag.build("ISaveTask");
    private IjkMediaPlayer mIjkMediaPlayer;
    private VideoSaveInfo v;
    private SaveFilters s;

    public SoftSaveTask(VideoSaveInfo v, SaveFilters s) {
        super();
        this.v = v;
        this.s = s;
    }

    @Override
    public void prepare() throws IOException {
        initPlayer();

    }

    private void initPlayer() throws IOException {
        mIjkMediaPlayer = createSaveModePlayer(false);
        mIjkMediaPlayer.setSaveInfo(v.isMediaCodec(), v.getVideoSavePath(),
                v.getOutputWidth(), v.getOutputHeight(), v.getOutputBitrate(), v.getFps());
        mIjkMediaPlayer.setGLFilter(s.isFilter());
        mIjkMediaPlayer.setOnPreparedListener(mPreparedListener);
        mIjkMediaPlayer.setOnCompletionListener(mCompletionListener);
        mIjkMediaPlayer.setDataSource(v.getSrcPath());
        mIjkMediaPlayer.prepareAsync();
    }

    private IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
        public void onPrepared(IMediaPlayer mp) {
            Log.d(TAG, "onPrepared");
            mIjkMediaPlayer.start();
        }
    };
    private IMediaPlayer.OnCompletionListener mCompletionListener =
            new IMediaPlayer.OnCompletionListener() {
                public void onCompletion(IMediaPlayer mp) {
                    Log.e(TAG, "OnCompletionListener\n");
                }
            };
}
