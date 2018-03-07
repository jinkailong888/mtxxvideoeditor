package com.meitu.library.videoeditor.watermark;

/**
 * Created by wyh3 on 2018/1/23.
 * WaterMarkInfo
 */


public class WaterMarkInfo {
    private String mImagePath;
    private int mWidth;
    private int mHeight;
    private int mStartTime;
    //若要设置水印与视频时长相同，mDuration字段设置为-1即可
    private int mDuration=-1;
    private boolean mVisible = true;
    @WaterMarkPosition.WaterMarkPos
    private int mWaterMarkPos;
    //left or right Padding
    private int mHorizontalPadding;
    //top or bottom Padding
    private int mVerticalPadding;


    public WaterMarkInfo() {
    }

    /**
     * 图片路径
     */
    public String getImagePath() {
        return mImagePath;
    }

    /**
     * 设置图片路径,支持实时更新图片素材路径。
     */
    public void setImagePath(String source) {
        mImagePath = source;
    }

    /**
     * 水印的宽度
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * 设置水印的宽度
     */
    public void setWidth(int width) {
        this.mWidth = width;
    }

    /**
     * 水印的高度
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * 设置水印的高度
     */
    public void setHeight(int height) {
        this.mHeight = height;
    }


    /**
     * 在要保存的视频中，开始播放的时间
     *
     * @return 单位：ms（视频1倍速下的时间）
     */
    public int getStartTime() {
        return mStartTime;
    }

    /**
     * 设置在要保存的视频中，开始播放的时间，目前没有设置时间的需求，等日后有需求，再开放
     *
     * @param startTime 开始时间（视频1倍速下的时间），单位ms
     */
    private void setStartTime(int startTime) {
        this.mStartTime = startTime;
    }

    /**
     * 水印时长
     *
     * @return 单位：ms（视频1倍速下的时间）
     */
    public int getDuration() {
        return mDuration;
    }

    /**
     * 设置水印时长，传入 -1 可设置为视频时长
     *
     * @param duration 单位：ms（视频1倍速下的时间）
     */
    private void setDuration(int duration) {
        this.mDuration = duration;
    }

    /**
     * 水印是否为可见
     *
     * @return true，可见；false，不可见
     */
    public boolean isVisible() {
        return mVisible;
    }

    /**
     * 水印是否为可见，默认为true，即可见。
     *
     * @param visible true,可见；false，不可见
     */
    public void setVisible(boolean visible) {
        mVisible = visible;
    }


    public int getWaterMarkPos() {
        return mWaterMarkPos;
    }

    public void setWaterMarkPos(@WaterMarkPosition.WaterMarkPos int waterMarkPos) {
        mWaterMarkPos = waterMarkPos;
    }

    public int getHorizontalPadding() {
        return mHorizontalPadding;
    }

    public void setHorizontalPadding(int horizontalPadding) {
        mHorizontalPadding = horizontalPadding;
    }

    public int getVerticalPadding() {
        return mVerticalPadding;
    }

    public void setVerticalPadding(int verticalPadding) {
        mVerticalPadding = verticalPadding;
    }
}
