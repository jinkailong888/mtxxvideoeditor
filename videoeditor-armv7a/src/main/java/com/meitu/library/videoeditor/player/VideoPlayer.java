package com.meitu.library.videoeditor.player;

import com.meitu.library.videoeditor.bgm.BgMusicInfo;
import com.meitu.library.videoeditor.core.VideoEditor;
import com.meitu.library.videoeditor.player.listener.OnPlayListener;
import com.meitu.library.videoeditor.player.listener.OnSaveListener;
import com.meitu.library.videoeditor.video.VideoSaveInfo;

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

    void setLooping(boolean looping);

    void save(VideoSaveInfo videoSaveInfo);

    void release();

    void setOnSaveListener(OnSaveListener onSaveListener);

    void setOnPlayListener(OnPlayListener onPlayListener);

    /**
     * 生命周期
     */
    void onPause();

    void onResume();


    /**
     * 获取播放器信息
     */
    boolean isPlaying();

    boolean isLooping();

    long getDuration();

    long getCurrentPosition();

    IjkMediaPlayer getIjkMediaPlayer();


    /**
     * 视频编辑相关
     */
    void setGLFilter(boolean open);

    void setBgMusic(String musicPath,
                    int startTime,
                    int duration,
                    float speed,
                    boolean loop);


    void setBgMusic(BgMusicInfo bgMusicInfo);

    void setVolume(float volume, float volume1);
}