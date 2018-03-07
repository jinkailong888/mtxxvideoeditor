package com.meitu.library.videoeditor.player;

import android.graphics.Bitmap;

import com.meitu.library.videoeditor.core.VideoEditor;
import com.meitu.library.videoeditor.player.listener.OnGetFrameListener;
import com.meitu.library.videoeditor.player.listener.OnPlayListener;
import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

import java.util.List;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * Created by wyh3 on 2018/1/23.
 * 播放器
 */

public interface VideoPlayer {

    void init(VideoEditor.Builder builder);

    /**
     * 操作相关
     */

    void setDataSource(String path);

    void prepare(boolean autoPlay);

    void start();

    void stop();

    void pause();

    void save(VideoSaveInfo videoSaveInfo);

    void release();

    void setLooping(boolean looping);

    void setOnSaveListener(OnSaveListener onSaveListener);

    void setOnPlayListener(OnPlayListener onPlayListener);


    /**
     * 获取播放器信息
     */
    boolean isPlaying();

    boolean isLooping();

    long getDuration();

    long getCurrentPosition();

    IjkMediaPlayer getIjkMediaPlayer();

}