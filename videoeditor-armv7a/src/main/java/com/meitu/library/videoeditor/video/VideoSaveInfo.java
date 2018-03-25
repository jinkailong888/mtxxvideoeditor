package com.meitu.library.videoeditor.video;

/**
 * Created by wyh3 on 2018/1/22.
 * 视频保存配置信息
 */

public class VideoSaveInfo {
    //原视频地址
    private String mSrcPath;
    // 视频输出宽度，默认与原视频相同
    private int mOutputWidth;
    // 视频输出高度，默认与原视频相同
    private int mOutputHeight;
    // 输出码率，默认为 2000 * 1000 + 1
    private int mOutputBitrate = 2000 * 1000 + 1;
    // 输出fps， 默认为30帧
    private int mFps = 30;
    // I帧间隔秒数
    private int mIFrameInterval = 1;
    //视频角度，应与原视频相同，否则会出问题
    private int mRotate;
    // 视频保存地址
    private String mVideoSavePath;
    //是否硬解
    private boolean mMediaCodec;
    //原音音量
    private float mVideoVolume = 1f;

    @Override
    public String toString() {
        return "VideoSaveInfo{" +
                "mSrcPath='" + mSrcPath + '\'' +
                ", mOutputWidth=" + mOutputWidth +
                ", mOutputHeight=" + mOutputHeight +
                ", mOutputBitrate=" + mOutputBitrate +
                ", mFps=" + mFps +
                ", mIFrameInterval=" + mIFrameInterval +
                ", mRotate=" + mRotate +
                ", mVideoSavePath='" + mVideoSavePath + '\'' +
                ", mMediaCodec=" + mMediaCodec +
                ", mVideoVolume=" + mVideoVolume +
                '}';
    }

    public float getVideoVolume() {
        return mVideoVolume;
    }

    public void setVideoVolume(float videoVolume) {
        mVideoVolume = videoVolume;
    }

    public int getRotate() {
        return mRotate;
    }

    public void setRotate(int rotate) {
        mRotate = rotate;
    }

    public String getSrcPath() {
        return mSrcPath;
    }

    public void setSrcPath(String srcPath) {
        mSrcPath = srcPath;
    }

    public int getIFrameInterval() {
        return mIFrameInterval;
    }

    public void setIFrameInterval(int IFrameInterval) {
        mIFrameInterval = IFrameInterval;
    }

    public boolean isMediaCodec() {
        return mMediaCodec;
    }

    public void setMediaCodec(boolean mediaCodec) {
        mMediaCodec = mediaCodec;
    }

    public String getVideoSavePath() {
        return mVideoSavePath;
    }

    public void setVideoSavePath(String videoSavePath) {
        this.mVideoSavePath = videoSavePath;
    }


    public void setFps(int fps) {
        mFps = fps;
    }

    public int getFps() {
        return mFps;
    }

    public void setOutputBitrate(int outputBitrate) {
        mOutputBitrate = outputBitrate;
    }

    public int getOutputBitrate() {
        return mOutputBitrate;
    }

    public int getOutputWidth() {
        return mOutputWidth;
    }

    public void setOutputWidth(int outputWidth) {
        this.mOutputWidth = outputWidth;
    }

    public int getOutputHeight() {
        return mOutputHeight;
    }

    public void setOutputHeight(int outputHeight) {
        this.mOutputHeight = outputHeight;
    }

}
