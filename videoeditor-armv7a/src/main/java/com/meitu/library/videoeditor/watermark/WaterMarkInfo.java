package com.meitu.library.videoeditor.watermark;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by wyh3 on 2018/1/23.
 * WaterMarkInfo
 */

//TODO 1.2.3.4 版本水印添加失效

public class WaterMarkInfo implements Parcelable {
    private String mImagePath;
    private int mWidth;
    private int mHeight;
    private String mConfigPath;
    private long mStartTime;
    private long mDuration=30000;
    private boolean mVisible = true;

    public WaterMarkInfo() {
    }

    protected WaterMarkInfo(Parcel in) {
        mImagePath = in.readString();
        mWidth = in.readInt();
        mHeight = in.readInt();
        mConfigPath = in.readString();
        mStartTime = in.readLong();
        mDuration = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mImagePath);
        dest.writeInt(mWidth);
        dest.writeInt(mHeight);
        dest.writeString(mConfigPath);
        dest.writeLong(mStartTime);
        dest.writeLong(mDuration);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WaterMarkInfo> CREATOR = new Creator<WaterMarkInfo>() {
        @Override
        public WaterMarkInfo createFromParcel(Parcel in) {
            return new WaterMarkInfo(in);
        }

        @Override
        public WaterMarkInfo[] newArray(int size) {
            return new WaterMarkInfo[size];
        }
    };

    /**
     * 图片路径
     */
    public String getImagePath() {
        return mImagePath;
    }

    /**
     * 设置图片路径,支持实时更新图片素材路径。
     * 需调用{@link com.meitu.library.media.core.MVEditor#updateWaterMark(WaterMarkInfo)}更新
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
     * 配置文件路径
     */
    public String getConfigPath() {
        return mConfigPath;
    }

    /**
     * 设置配置文件路径
     */
    public void setConfigPath(String plistPath) {
        this.mConfigPath = plistPath;
    }

    /**
     * 在要保存的视频中，开始播放的时间
     *
     * @return 单位：ms（视频1倍速下的时间）
     */
    public long getStartTime() {
        return mStartTime;
    }

    /**
     * 设置在要保存的视频中，开始播放的时间，目前没有设置时间的需求，等日后有需求，再开放
     *
     * @param startTime 开始时间（视频1倍速下的时间），单位ms
     */
    private void setStartTime(long startTime) {
        this.mStartTime = startTime;
    }

    /**
     * 设置播放时长
     *
     * @return 单位：ms（视频1倍速下的时间）
     */
    public long getDuration() {
        return mDuration;
    }

    /**
     * 设置播放时长，目前没有设置时间的需求，等日后有需求，再开放
     *
     * @param duration 单位：ms（视频1倍速下的时间）
     */
    private void setDuration(long duration) {
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
     * 支持实时更新水印可见性，需要调用{@link com.meitu.library.media.core.MVEditor#updateWaterMark(WaterMarkInfo)}更新
     *
     * @param visible true,可见；false，不可见
     */
    public void setVisible(boolean visible) {
        mVisible = visible;
    }





}
