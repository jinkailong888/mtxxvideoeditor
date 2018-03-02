package com.meitu.library.videoeditor.player;

/**
 * Created by wyh3 on 2018/1/25.
 * 播放策略
 */

public class PlayerStrategyInfo {

    //循环播放
    private boolean looping = true;
    //prepare之后是否自动播放
    private boolean prepareAutoPlay = true;
    //是否需要在prepare时自动显示视频第一帧
    private boolean showFirstFrame = true;
    //获取播放器进度时间间隔
    private long updateProgressInterval = 50;
    //等待bitmap被绘制到屏幕上时间
    private static final int waitBitmapShowTime = 64;


    public boolean isLooping() {
        return looping;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public boolean isPrepareAutoPlay() {
        return prepareAutoPlay;
    }

    public void setPrepareAutoPlay(boolean prepareAutoPlay) {
        this.prepareAutoPlay = prepareAutoPlay;
    }

    public boolean isShowFirstFrame() {
        return showFirstFrame;
    }

    public void setShowFirstFrame(boolean showFirstFrame) {
        this.showFirstFrame = showFirstFrame;
    }

    public long getUpdateProgressInterval() {
        return updateProgressInterval;
    }

    public void setUpdateProgressInterval(long updateProgressInterval) {
        this.updateProgressInterval = updateProgressInterval;
    }

    public int getWaitBitmapShowTime() {
        return waitBitmapShowTime;
    }

}
