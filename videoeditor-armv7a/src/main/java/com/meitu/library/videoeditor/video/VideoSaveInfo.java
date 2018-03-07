package com.meitu.library.videoeditor.video;

/**
 * Created by wyh3 on 2018/1/22.
 * 视频保存配置信息
 */

public class VideoSaveInfo  {
    // 视频输出宽度
    private int mOutputWidth;
    // 视频输出高度
    private int mOutputHeight;
    // 输出码率，默认为 2000 * 1000 + 1
    private int mOutputBitrate = 2000 * 1000 + 1;
    // 设置播放器进入保存模式后视频的保存帧率,底层默认fps是30帧
    private int mFps = 30;
    // 视频保存地址
    private String mVideoSavePath;
    //是否硬解
    private boolean mMediaCodec;


    @Override
    public String toString() {
        return "VideoSaveInfo{" +
                "mOutputWidth=" + mOutputWidth +
                ", mOutputHeight=" + mOutputHeight +
                ", mOutputBitrate=" + mOutputBitrate +
                ", mFps=" + mFps +
                ", mVideoSavePath='" + mVideoSavePath + '\'' +
                ", mMediaCodec=" + mMediaCodec +
                '}';
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
