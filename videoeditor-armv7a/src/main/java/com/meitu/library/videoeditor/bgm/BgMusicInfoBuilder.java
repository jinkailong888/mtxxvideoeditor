package com.meitu.library.videoeditor.bgm;

import com.meitu.library.videoeditor.core.VideoEditor;

/**
 * Created by wyh3 on 2018/1/26.
 * BgMusicInfo构造器
 */

public class BgMusicInfoBuilder {

    private VideoEditor mVideoEditor;
    private BgMusicInfo mBgMusicInfo;

    public BgMusicInfoBuilder(VideoEditor videoEditor) {
        mVideoEditor = videoEditor;
        mBgMusicInfo = new BgMusicInfo();
    }

    public BgMusicInfoBuilder setSpeed(float speed) {
        mBgMusicInfo.setSpeed(speed);
        return this;
    }

    public BgMusicInfoBuilder setMusicPath(String musicPath) {
        mBgMusicInfo.setMusicPath(musicPath);
        return this;
    }

    private BgMusicInfoBuilder setStartTime(long startTime) {
        mBgMusicInfo.setStartTime(startTime);
        return this;
    }

    private BgMusicInfoBuilder setSourceStartTime(long sourceStartTime) {
        mBgMusicInfo.setSourceStartTime(sourceStartTime);
        return this;
    }

    private BgMusicInfoBuilder setDuration(long duration) {
        mBgMusicInfo.setDuration(duration);
        return this;
    }

    public BgMusicInfoBuilder setRepeat(boolean repeat) {
        mBgMusicInfo.setRepeat(repeat);
        return this;
    }

    public void setBgMusic() {
        mVideoEditor.setBgMusic(mBgMusicInfo);
    }

}
