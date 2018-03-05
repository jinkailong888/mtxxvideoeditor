package com.meitu.library.videoeditor.player;

import android.graphics.Bitmap;

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

    /**
     * 操作相关
     */


    void setDataSource(List<String> path);

    void setDataSource(String path);

    void setShowWidth(int showWidth);

    void setShowHeight(int showHeight);

    void prepare(boolean autoPlay);

    void play();

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

    long getRawDuration();

    long getCurrentPosition();

    long getRawCurrentPosition();

    IjkMediaPlayer getIjkMediaPlayer();


    /**
     * 获取帧信息
     */
    Bitmap getFirstFrame();

    void getCurrentFrame(final OnGetFrameListener listener);

}