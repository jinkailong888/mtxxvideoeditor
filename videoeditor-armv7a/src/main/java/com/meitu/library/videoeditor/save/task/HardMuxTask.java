package com.meitu.library.videoeditor.save.task;

import android.util.Log;

import com.meitu.library.videoeditor.save.bean.SaveFilters;
import com.meitu.library.videoeditor.save.encode.HardMuxJni;
import com.meitu.library.videoeditor.save.muxer.Mp4MuxStore;
import com.meitu.library.videoeditor.save.muxer.MuxStore;
import com.meitu.library.videoeditor.util.Tag;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.io.IOException;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by wyh3 on 2018/3/26.
 * 软解硬保
 */

public class HardMuxTask extends ISaveTask{

    private final static String TAG = Tag.build("HardSaveTask");

    private VideoSaveInfo v;
    private SaveFilters s;
    private MuxStore mMuxStore;
    private Object VideoWroteLock = new Object();
    private HardMuxJni mHardMuxJni;
    private IjkMediaPlayer mIjkMediaPlayer;


    @Override
    public void prepare() throws IOException {
        mMuxStore = new Mp4MuxStore(v.getVideoSavePath(), v.getRotate());
        mHardMuxJni = new HardMuxJni();
        mIjkMediaPlayer = createSaveModePlayer(true);
        mIjkMediaPlayer.setHardMuxListener(mHardMuxJni);
        mIjkMediaPlayer.setSaveInfo(v.isMediaCodec(), v.getVideoSavePath(),
                v.getOutputWidth(), v.getOutputHeight(), v.getOutputBitrate(), v.getFps());
        mIjkMediaPlayer.setGLFilter(s.isFilter());
        mIjkMediaPlayer.setOnPreparedListener(mPreparedListener);
        mIjkMediaPlayer.setOnCompletionListener(mCompletionListener);
        mIjkMediaPlayer.setDataSource(v.getSrcPath());
        mIjkMediaPlayer.prepareAsync();
    }

    public HardMuxTask(VideoSaveInfo v, SaveFilters s) {
        super();
        this.v = v;
        this.s = s;
    }

    @Override
    public void run() {
        try {
            prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
