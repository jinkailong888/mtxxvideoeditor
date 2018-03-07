package com.meitu.library.videoeditor.bgm;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;


public class BgMusicInfo {

    //速度默认为1.0
    private float mSpeed = 1.0f;
    private String mMusicPath;
    private int mStartTime;
    private long mSourceStartTime;
    private int mDuration = 30000;  //todo 设置什么值可以全首播？
    private boolean mLoop = true;


    public BgMusicInfo() {
    }

    /**
     * 获取音乐文件路径
     */
    public String getMusicPath() {
        return mMusicPath;
    }

    /**
     * 设置音乐文件路径
     */
    public void setMusicPath(@NonNull String musicPath) {
        this.mMusicPath = musicPath;
    }

    public int getStartTime() {
        return mStartTime;
    }

    /**
     * 设置在TimeLine上的开始时间，暂时没有启用，日后有需求，需要联系底层同学处理
     */
    public void setStartTime(int startTime) {
        this.mStartTime = startTime;
    }

    public int getDuration() {
        return mDuration;
    }

    /**
     * 设置播放时长，目前组件不会对其做任何处理，直接设置给底层
     *
     * @param duration 加速后的时长
     */
    public void setDuration(int duration) {
        this.mDuration = duration;
    }

    public long getSourceStartTime() {
        return mSourceStartTime;
    }

    /**
     * 设置音乐源 播放的起始偏移位置，即歌曲从什么位置开始播放
     */
    public void setSourceStartTime(long startTime) {
        this.mSourceStartTime = startTime;
    }

    /**
     * 视频未结束时，是否循环播放背景音乐
     *
     * @return true 循环播放; false 不循环播放
     */
    public boolean isRepeat() {
        return mLoop;
    }

    /**
     * 视频未结束时，是否循环播放背景音乐
     *
     * @param repeat true 循环播放; false 不循环播放
     */
    public void setRepeat(boolean repeat) {
        this.mLoop = repeat;
    }

    /**
     * 设置速度
     */
    public float getSpeed() {
        return mSpeed;
    }

    /**
     * 设置速度，默认为1.0
     */
    public void setSpeed(float speed) {
        this.mSpeed = speed;
    }

}
